-- 피드 등록 시점의 날씨 스냅샷에 최저/최고 기온 컬럼 추가
-- WeatherSummaryDto.temperature가 min/max를 요구하나 V1 feeds 스냅샷 컬럼에 없었음.
-- nullable: 기존 등록된 피드는 min/max가 없으므로

ALTER TABLE feeds
    ADD COLUMN temperature_min DOUBLE PRECISION;

ALTER TABLE feeds
    ADD COLUMN temperature_max DOUBLE PRECISION;