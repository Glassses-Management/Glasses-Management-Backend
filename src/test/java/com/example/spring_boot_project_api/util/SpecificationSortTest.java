package com.example.spring_boot_project_api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Guards the sort path against the failure that made underscored fields
 * unusable.
 *
 * <p>Spring Data splits a sort property on "." and on "_", so a field such as
 * {@code order_date} or {@code sale_price} can never be reached through a
 * {@link Pageable}: it is read as the path {@code order -> date} and the lookup
 * throws {@code PropertyReferenceException}, which nothing maps, so the endpoint
 * answered 500. Ordering is therefore expressed on the query instead, which is
 * what these tests pin down.
 */
class SpecificationSortTest {

    @Test
    void apply_leavesAnUnsortableSpecificationAlone() {
        Specification<Object> spec = (root, query, cb) -> cb.conjunction();

        assertSame(spec, SpecificationSort.apply(spec, Sort.unsorted()));
    }

    @Test
    void apply_ordersByEverySortKeyInTheGivenOrder() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Object> root = mock(Root.class);
        Path<Object> datePath = mock(Path.class);
        Path<Object> statusPath = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        Order asc = mock(Order.class);
        Order desc = mock(Order.class);

        when(query.getResultType()).thenAnswer(invocation -> Object.class);
        when(root.<Object>get("order_date")).thenReturn(datePath);
        when(root.<Object>get("status")).thenReturn(statusPath);
        when(cb.asc(datePath)).thenReturn(asc);
        when(cb.desc(statusPath)).thenReturn(desc);
        when(cb.conjunction()).thenReturn(predicate);

        Sort sort = Sort.by(Sort.Order.asc("order_date"), Sort.Order.desc("status"));
        SpecificationSort.apply((Specification<Object>) (r, q, c) -> predicate, sort)
                .toPredicate(root, query, cb);

        // The property name is handed to Root.get untouched, which is the whole
        // point: the metamodel resolves it even though the Pageable cannot.
        verify(root).get("order_date");
        verify(root).get("status");
        verify(cb).asc(datePath);
        verify(cb).desc(statusPath);
        verify(query).orderBy(asc, desc);
    }

    @Test
    void apply_doesNotOrderTheCountQuery() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Long> countQuery = mock(CriteriaQuery.class);
        Root<Object> root = mock(Root.class);
        Predicate predicate = mock(Predicate.class);

        // Spring Data evaluates the same Specification for the count query, where
        // the result type is Long rather than the entity.
        when(countQuery.getResultType()).thenAnswer(invocation -> Long.class);
        when(cb.conjunction()).thenReturn(predicate);

        SpecificationSort.apply((Specification<Object>) (r, q, c) -> predicate,
                Sort.by(Sort.Order.desc("order_date")))
                .toPredicate(root, countQuery, cb);

        // Order[] rather than any(), because orderBy also accepts Expression...
        verify(countQuery, never()).orderBy(any(Order[].class));
        verifyNoInteractions(root);
    }

    @Test
    void withoutSort_keepsTheSamePageAndSizeAndDropsTheSort() {
        Pageable sorted = PageRequest.of(3, 25, Sort.by(Sort.Order.desc("order_date")));

        Pageable result = SpecificationSort.withoutSort(sorted);

        assertEquals(3, result.getPageNumber());
        assertEquals(25, result.getPageSize());
        assertEquals(75L, result.getOffset());
        assertEquals(Sort.unsorted(), result.getSort());
    }

    @Test
    void withoutSort_passesAnUnpagedRequestStraightThrough() {
        Pageable unpaged = Pageable.unpaged();

        assertSame(unpaged, SpecificationSort.withoutSort(unpaged));
    }

    @Test
    void withoutSort_keepsTheFirstPageOfAnUnsortedRequest() {
        Pageable plain = PageRequest.of(0, 20);

        Pageable result = SpecificationSort.withoutSort(plain);

        assertEquals(0, result.getPageNumber());
        assertEquals(20, result.getPageSize());
        assertEquals(Sort.unsorted(), result.getSort());
    }

    @Test
    void apply_supportsAnEmptySortListWithoutFailing() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Object> root = mock(Root.class);
        Predicate predicate = mock(Predicate.class);

        when(query.getResultType()).thenAnswer(invocation -> Object.class);

        SpecificationSort.apply((Specification<Object>) (r, q, c) -> predicate, Sort.unsorted())
                .toPredicate(root, query, cb);

        verifyNoInteractions(root);
    }

    @Test
    void orderingsAreDistinctObjectsForEachKey() {
        // Guards the array conversion: an empty or single-element result must
        // still reach orderBy without an ArrayStoreException.
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Object> root = mock(Root.class);
        Path<Object> path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        Order asc = mock(Order.class);

        when(query.getResultType()).thenAnswer(invocation -> Object.class);
        when(root.<Object>get("sale_price")).thenReturn(path);
        when(cb.asc(path)).thenReturn(asc);

        SpecificationSort.apply((Specification<Object>) (r, q, c) -> predicate,
                Sort.by(Sort.Order.asc("sale_price")))
                .toPredicate(root, query, cb);

        verify(query).orderBy(asc);
        verify(root).get("sale_price");
    }

    @Test
    void apply_stillEvaluatesTheWrappedSpecification() {
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Object> root = mock(Root.class);
        Path<Object> path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        Order desc = mock(Order.class);

        when(query.getResultType()).thenAnswer(invocation -> Object.class);
        when(root.<Object>get("order_date")).thenReturn(path);
        when(cb.desc(path)).thenReturn(desc);

        Specification<Object> wrapped = SpecificationSort.apply(
                (Specification<Object>) (r, q, c) -> predicate, Sort.by(Sort.Order.desc("order_date")));

        // The filters must still run, otherwise the endpoint would ignore
        // dateFrom/dateTo while sorting correctly.
        assertSame(predicate, wrapped.toPredicate(root, query, cb));
    }
}
