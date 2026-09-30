package com.example.spring_boot_project_api.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

/**
 * Applies a {@link Sort} to a {@link Specification} rather than to the
 * {@link Pageable}.
 *
 * <p>Spring Data resolves a sort property against the entity before it builds the
 * query, and it splits that property on "." and on "_"
 * ({@code SimplePropertyPath.SPLITTER}). A field whose Java name contains an
 * underscore - {@code order_date}, {@code sale_price} - is therefore read as the
 * two-segment path {@code order -> date}, which no entity satisfies, and the
 * lookup fails with {@code PropertyReferenceException}. Nothing maps that
 * exception, so the request is reported as a 500 even though the sort was on the
 * endpoint's own allowlist.
 *
 * <p>The Criteria API is unaffected: {@link Root#get} resolves the name against
 * the metamodel exactly as written. So the ordering is expressed on the query the
 * Specification is handed, which can name every attribute, and the Pageable is
 * handed on paged but unsorted. Leaving the sort on the Pageable is precisely what
 * makes Spring Data resolve the property and throw.
 *
 * <p>The allowlist stays the only gate on which property names a caller may use,
 * so this widens nothing: it only lets an already-allowed name reach the database.
 */
public final class SpecificationSort {

    private SpecificationSort() {
    }

    /**
     * Wraps {@code spec} so the query it builds carries {@code sort}.
     *
     * <p>Multi-key sorts keep the order they were given in, and an unsorted
     * {@code sort} returns the specification unchanged.
     */
    public static <T> Specification<T> apply(Specification<T> spec, Sort sort) {
        if (sort.isUnsorted()) {
            return spec;
        }

        return (root, query, cb) -> {
            // The same Specification is evaluated a second time for the count
            // query, where an ORDER BY has no meaning, so only the paged query is
            // sorted. The count itself is unaffected either way.
            if (query != null && !Long.class.equals(query.getResultType())) {
                query.orderBy(orders(root, cb, sort));
            }
            return spec.toPredicate(root, query, cb);
        };
    }

    /**
     * The same page and page size as {@code pageable}, with the sort removed.
     *
     * <p>Page and size are carried over untouched, so pagination behaves exactly
     * as before; only the sort is taken off, because Spring Data builds its own
     * ORDER BY from it and resolving that property is the failure this avoids.
     */
    public static Pageable withoutSort(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    private static <T> Order[] orders(Root<T> root, CriteriaBuilder cb, Sort sort) {
        List<Order> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            // Safe by construction: callers check the name against their
            // allowlist first, and the name is a real metamodel attribute, which
            // is what Root.get needs and what the old path could not manage.
            Path<?> path = root.get(order.getProperty());
            orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
        }
        return orders.toArray(new Order[0]);
    }
}
