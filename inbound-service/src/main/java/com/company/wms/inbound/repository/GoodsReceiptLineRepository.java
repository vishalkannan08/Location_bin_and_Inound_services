package com.company.wms.inbound.repository;

import com.company.wms.inbound.domain.GoodsReceiptLine;
import com.company.wms.inbound.domain.GoodsReceiptLineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoodsReceiptLineRepository extends JpaRepository<GoodsReceiptLine, UUID> {

    /**
     * Loads a line with its parent receipt. putaway-service asks for a line by
     * id, and the response needs the receipt's warehouse and number.
     */
    @Query("select l from GoodsReceiptLine l join fetch l.goodsReceipt where l.id = :id")
    Optional<GoodsReceiptLine> findByIdWithReceipt(@Param("id") UUID id);

    /**
     * The put-away work queue: lines with stock sitting on the dock.
     * This is what putaway-service polls to build its task list.
     */
    @Query("""
            select l from GoodsReceiptLine l
            join fetch l.goodsReceipt r
            where r.warehouseId = :warehouseId
              and l.status in :statuses
              and l.receivedQuantity > l.putAwayQuantity
            order by r.receivedAt asc, l.lineNumber asc
            """)
    List<GoodsReceiptLine> findAwaitingPutAway(@Param("warehouseId") UUID warehouseId,
                                               @Param("statuses") List<GoodsReceiptLineStatus> statuses);

    List<GoodsReceiptLine> findBySkuIdOrderByCreatedAtDesc(String skuId);
}
