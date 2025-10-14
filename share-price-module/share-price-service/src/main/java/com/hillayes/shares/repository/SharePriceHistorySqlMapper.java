package com.hillayes.shares.repository;

import com.hillayes.shares.domain.SharePriceHistory;

import java.util.LinkedHashMap;
import java.util.Map;

public class SharePriceHistorySqlMapper extends SqlEntityMapper<SharePriceHistory> {
    @Override
    public Map<String, ColMapper<SharePriceHistory>> initColMappings() {
        LinkedHashMap<String, ColMapper<SharePriceHistory>> result = LinkedHashMap.newLinkedHashMap(7);
        result.put("share_index_id", (s, offset, entity) -> setUuid(s, offset + 1, entity.getId().getShareIndexId()));
        result.put("resolution", (s, offset, entity) -> setString(s, offset + 2, entity.getId().getResolution().name()));
        result.put("market_date", (s, offset, entity) -> setDate(s, offset + 3, entity.getId().getDate()));
        result.put("open_price", (s, offset, entity) -> setDecimal(s, offset + 4, entity.getOpen()));
        result.put("high_price", (s, offset, entity) -> setDecimal(s, offset + 5, entity.getHigh()));
        result.put("low_price", (s, offset, entity) -> setDecimal(s, offset + 6, entity.getLow()));
        result.put("close_price", (s, offset, entity) -> setDecimal(s, offset + 7, entity.getClose()));

        return result;
    }
}
