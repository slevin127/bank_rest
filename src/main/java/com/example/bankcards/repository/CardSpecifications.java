package com.example.bankcards.repository;

import com.example.bankcards.dto.card.CardFilterRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class CardSpecifications {

    private CardSpecifications() {
    }

    public static Specification<Card> withFilter(CardFilterRequest filter) {
        return (root, query, builder) -> {
            if (filter == null) {
                return builder.conjunction();
            }
            List<Predicate> predicates = new ArrayList<>();
            if (filter.hasOwnerFilter()) {
                predicates.add(builder.equal(root.get("owner").get("id"), filter.getOwnerId()));
            }
            if (filter.hasStatusFilter()) {
                predicates.add(builder.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.hasMaskedNumberFilter()) {
                String likePattern = "%" + filter.getMaskedNumber().replace("*", "%").toLowerCase() + "%";
                predicates.add(builder.like(builder.lower(root.get("maskedNumber")), likePattern));
            }
            if (filter.hasMinBalanceFilter()) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("balance"), filter.getMinBalance()));
            }
            if (filter.hasMaxBalanceFilter()) {
                predicates.add(builder.lessThanOrEqualTo(root.get("balance"), filter.getMaxBalance()));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Card> withStatuses(List<CardStatus> statuses) {
        return (root, query, builder) -> {
            if (statuses == null || statuses.isEmpty()) {
                return builder.conjunction();
            }
            CriteriaBuilder.In<CardStatus> in = builder.in(root.get("status"));
            statuses.forEach(in::value);
            return in;
        };
    }

    public static Specification<Card> balanceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, builder) -> {
            if (min == null && max == null) {
                return builder.conjunction();
            }
            if (min != null && max != null) {
                return builder.between(root.get("balance"), min, max);
            }
            if (min != null) {
                return builder.greaterThanOrEqualTo(root.get("balance"), min);
            }
            return builder.lessThanOrEqualTo(root.get("balance"), max);
        };
    }
}
