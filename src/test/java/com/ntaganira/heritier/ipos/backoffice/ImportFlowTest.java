package com.ntaganira.heritier.ipos.backoffice;

import com.ntaganira.heritier.ipos.backoffice.dto.ImportForm;
import com.ntaganira.heritier.ipos.backoffice.entity.ImportBatch;
import com.ntaganira.heritier.ipos.backoffice.entity.InventoryMovement;
import com.ntaganira.heritier.ipos.backoffice.entity.Product;
import com.ntaganira.heritier.ipos.backoffice.enums.ImportStatus;
import com.ntaganira.heritier.ipos.backoffice.enums.MovementType;
import com.ntaganira.heritier.ipos.backoffice.repository.InventoryMovementRepository;
import com.ntaganira.heritier.ipos.backoffice.repository.ProductRepository;
import com.ntaganira.heritier.ipos.backoffice.service.ImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImportFlowTest extends AbstractIntegrationTest {

    @Autowired
    private ImportService importService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryMovementRepository movementRepository;

    @Test
    void receivingAnImportAddsStockAtLandedCost() {
        Product product = productRepository.findByActiveTrueOrderByNameAsc().get(0);
        BigDecimal stockBefore = product.getStockQuantity();

        ImportForm form = new ImportForm();
        form.setOriginCountry("China");
        form.setCurrency("USD");
        form.setExchangeRate(new BigDecimal("1000"));
        form.setFreight(new BigDecimal("500"));
        ImportForm.Line line = new ImportForm.Line();
        line.setProductId(product.getId());
        line.setQuantity(new BigDecimal("2"));
        line.setUnitCostForeign(new BigDecimal("10"));
        form.setLines(List.of(line));

        ImportBatch batch = importService.create(form);

        assertThat(batch.getStatus()).isEqualTo(ImportStatus.DRAFT);
        assertThat(batch.getTotalLanded()).isEqualByComparingTo("20500.00");
        assertThat(batch.getItems().get(0).getLandedUnitCost()).isEqualByComparingTo("10250.00");

        importService.receive(batch.getId());

        ImportBatch received = importService.get(batch.getId());
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        List<InventoryMovement> movements = movementRepository
                .findByProductIdOrderByOccurredAtDesc(product.getId());

        assertThat(received.getStatus()).isEqualTo(ImportStatus.RECEIVED);
        assertThat(updated.getStockQuantity()).isEqualByComparingTo(stockBefore.add(new BigDecimal("2")));
        assertThat(updated.getBuyingPrice()).isEqualByComparingTo("10250.00");
        assertThat(movements).anySatisfy(movement -> {
            assertThat(movement.getMovementType()).isEqualTo(MovementType.IMPORT);
            assertThat(movement.getReferenceId()).isEqualTo(batch.getReferenceNo());
            assertThat(movement.getQuantityDelta()).isEqualByComparingTo("2");
        });
    }
}
