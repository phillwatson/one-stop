package com.hillayes.shares.repository;

import com.hillayes.shares.domain.PriceHistory;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class PriceHistorySqlMapper extends SqlEntityMapper<PriceHistory> {
    @Override
    public Map<String, ColMapper<PriceHistory>> initColMappings() {
        LinkedHashMap<String, ColMapper<PriceHistory>> result = LinkedHashMap.newLinkedHashMap(7);
        result.put("share_index_id", (s, offset, entity) -> setUuid(s, offset, entity.getId().getShareIndexId()));
        result.put("resolution", (s, offset, entity) -> setString(s, offset, entity.getId().getResolution().name()));
        result.put("market_date", (s, offset, entity) -> setDate(s, offset, entity.getId().getDate()));
        result.put("open_price", (s, offset, entity) -> setDecimal(s, offset, entity.getOpen()));
        result.put("high_price", (s, offset, entity) -> setDecimal(s, offset, entity.getHigh()));
        result.put("low_price", (s, offset, entity) -> setDecimal(s, offset, entity.getLow()));
        result.put("close_price", (s, offset, entity) -> setDecimal(s, offset, entity.getClose()));

        return result;
    }
}
