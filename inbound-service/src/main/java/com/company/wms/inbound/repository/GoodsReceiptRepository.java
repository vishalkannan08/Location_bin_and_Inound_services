package com.company.wms.inbound.repository;

import com.company.wms.inbound.domain.GoodsReceipt;
import com.company.wms.inbound.domain.GoodsReceiptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, UUID> {

    Optional<GoodsReceipt> findByReceiptNumber(String receiptNumber);

    Page<GoodsReceipt> findByWarehouseId(UUID warehouseId, Pageable pageable);

    Page<GoodsReceipt> findByWarehouseIdAndStatus(UUID warehouseId, GoodsReceiptStatus status, Pageable pageable);

    List<GoodsReceipt> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);

    /**
     * Loads the receipt with its lines in one query. Every read of a receipt
     * touches its lines, and open-in-view is off, so a plain findById would
     * either N+1 or blow up outside the transaction.
     */
    @Query("select distinct r from GoodsReceipt r left join fetch r.lines where r.id = :id")
    Optional<GoodsReceipt> findByIdWithLines(UUID id);

    /** Receipt numbers come from a database sequence, so concurrent creates cannot collide. */
    @Query(value = "SELECT nextval('goods_receipt_number_seq')", nativeQuery = true)
    Long nextReceiptSequence();
}
