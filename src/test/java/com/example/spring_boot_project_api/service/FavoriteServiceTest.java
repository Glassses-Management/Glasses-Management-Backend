package com.example.spring_boot_project_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.example.spring_boot_project_api.mapper.FavoriteMapper;
import com.example.spring_boot_project_api.mapper.ProductMapper;
import com.example.spring_boot_project_api.model.Customer;
import com.example.spring_boot_project_api.model.Favorite;
import com.example.spring_boot_project_api.model.Product;
import com.example.spring_boot_project_api.repository.CustomerRepository;
import com.example.spring_boot_project_api.repository.FavoriteRepository;
import com.example.spring_boot_project_api.repository.ProductRepository;

/**
 * The wishlist is per customer, and that is enforced entirely on the server from
 * the token. These tests cover the properties that make that true: the owner is
 * read from the token, a repeat favourite does not duplicate, and one customer
 * can neither read nor delete another customer's rows.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FavoriteServiceTest {

    private static final Long USER_ID = 7L;
    private static final Long CUSTOMER_ID = 70L;
    private static final Long PRODUCT_ID = 34L;

    @Mock private FavoriteRepository favoriteRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private FavoriteMapper favoriteMapper;

    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(favoriteRepository, productRepository,
                customerRepository, favoriteMapper);
    }

    private Authentication authFor(Long userId) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("someone@example.com")
                .claim("uid", String.valueOf(userId))
                .claim("role", "CUSTOMER")
                .build();
        // The authorities constructor is the one that marks the token
        // authenticated, matching what the resource server filter produces.
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    private void customerExists() {
        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        when(customerRepository.findByUserId(USER_ID)).thenReturn(Optional.of(customer));
    }

    @Test
    void addingStoresTheFavoriteAgainstTheCustomerFromTheToken() {
        customerExists();
        Product product = new Product();
        product.setId(PRODUCT_ID);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(favoriteRepository.findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID))
                .thenReturn(Optional.empty());
        when(favoriteRepository.save(any(Favorite.class))).thenAnswer(call -> {
            Favorite saved = call.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        favoriteService.add(authFor(USER_ID), PRODUCT_ID);

        ArgumentCaptor<Favorite> saved = ArgumentCaptor.forClass(Favorite.class);
        verify(favoriteRepository).save(saved.capture());

        assertThat(saved.getValue().getCustomer().getId()).isEqualTo(CUSTOMER_ID);
        assertThat(saved.getValue().getProduct().getId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    void favouritingTwiceDoesNotCreateASecondRow() {
        customerExists();
        Favorite existing = new Favorite();
        existing.setId(55L);
        when(favoriteRepository.findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID))
                .thenReturn(Optional.of(existing));

        favoriteService.add(authFor(USER_ID), PRODUCT_ID);

        // The unique constraint would stop the duplicate anyway, but the check
        // means the caller gets the row it already had instead of an error.
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void favouritingAnUnknownProductIsNotFound() {
        customerExists();
        when(favoriteRepository.findByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID))
                .thenReturn(Optional.empty());
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.add(authFor(USER_ID), PRODUCT_ID))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);

        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void deletingIsScopedToTheOwnerSoAnotherCustomersRowSurvives() {
        customerExists();
        when(favoriteRepository.deleteByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID))
                .thenReturn(1L);

        favoriteService.remove(authFor(USER_ID), PRODUCT_ID);

        // The customer id in the delete comes from the token, never the request.
        verify(favoriteRepository).deleteByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID);
    }

    @Test
    void deletingSomethingThatWasNeverSavedIsNotAnError() {
        customerExists();
        when(favoriteRepository.deleteByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID))
                .thenReturn(0L);

        favoriteService.remove(authFor(USER_ID), PRODUCT_ID);

        verify(favoriteRepository, times(1))
                .deleteByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID);
    }

    @Test
    void listingOnlyEverReturnsTheCallersOwnFavourites() {
        customerExists();
        when(favoriteRepository.findByCustomerIdOrderByCreatedAtDesc(CUSTOMER_ID))
                .thenReturn(List.of());

        favoriteService.list(authFor(USER_ID));

        verify(favoriteRepository).findByCustomerIdOrderByCreatedAtDesc(CUSTOMER_ID);
    }

    @Test
    void theCheckEndpointAlsoUsesTheTokenOwner() {
        customerExists();
        when(favoriteRepository.existsByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID))
                .thenReturn(true);

        assertThat(favoriteService.isFavorite(authFor(USER_ID), PRODUCT_ID)).isTrue();

        verify(favoriteRepository).existsByCustomerIdAndProductId(CUSTOMER_ID, PRODUCT_ID);
    }

    @Test
    void anAnonymousCallerIsRejectedBeforeAnyLookup() {
        Authentication anonymous = new AnonymousAuthenticationToken("key", "anonymous",
                List.of(() -> "ROLE_ANONYMOUS"));

        assertThatThrownBy(() -> favoriteService.list(anonymous))
                .isInstanceOf(IllegalStateException.class);

        verify(customerRepository, never()).findByUserId(any());
    }

    @Test
    void aTokenWithNoCustomerProfileIsNotFoundRatherThanEmpty() {
        when(customerRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> favoriteService.list(authFor(USER_ID)))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }
}
