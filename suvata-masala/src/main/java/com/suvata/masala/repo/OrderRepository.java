package com.suvata.masala.repo;

import com.suvata.masala.domain.CustomerOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<CustomerOrder> findByOrderRef(String orderRef);

    @EntityGraph(attributePaths = "items")
    Optional<CustomerOrder> findByRazorpayOrderId(String razorpayOrderId);

    boolean existsByOrderRef(String orderRef);
}
