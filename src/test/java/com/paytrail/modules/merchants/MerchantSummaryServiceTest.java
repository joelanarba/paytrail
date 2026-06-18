package com.paytrail.modules.merchants;

import com.paytrail.document.MerchantSummary;
import com.paytrail.exception.ForbiddenException;
import com.paytrail.filter.MerchantContext;
import com.paytrail.modules.merchants.service.MerchantSummaryService;
import com.paytrail.repository.MerchantSummaryRepository;
import org.junit.jupiter.api.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MerchantSummaryServiceTest {
    private final MerchantSummaryRepository repo = mock(MerchantSummaryRepository.class);
    private final MerchantSummaryService service = new MerchantSummaryService(repo);

    @AfterEach void clear() { MerchantContext.clear(); }

    @Test
    void returnsZeroedWhenNoSummary() {
        MerchantContext.set("m1", false);
        when(repo.findByMerchantId("m1")).thenReturn(Optional.empty());
        assertEquals(0L, service.getCurrentMerchantSummary().getTotalTransactions());
    }

    @Test
    void nonSuperCannotReadOtherMerchant() {
        MerchantContext.set("m1", false);
        assertThrows(ForbiddenException.class, () -> service.getMerchantSummary("m2"));
    }

    @Test
    void superCanReadAnyMerchant() {
        MerchantContext.set(null, true);
        MerchantSummary s = new MerchantSummary(); s.setMerchantId("m9"); s.setTotalTransactions(3);
        when(repo.findByMerchantId("m9")).thenReturn(Optional.of(s));
        assertEquals(3L, service.getMerchantSummary("m9").getTotalTransactions());
    }
}
