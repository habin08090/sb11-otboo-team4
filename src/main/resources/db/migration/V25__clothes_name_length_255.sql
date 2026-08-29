-- 구매 링크에서 추출한 상품명은 쇼핑몰 페이지 제목을 그대로 가져와 50자를 쉽게 넘는다.
-- (예: "피지컬 디파트먼트(PHYSICAL DEPARTMENT) 나일론 사커 롱슬리브_다크 블루 - 사이즈 & 후기 | 무신사" — 65자)
-- DTO 제약(@Size(max = 255))과 같은 값으로 맞춘다.
ALTER TABLE clothes
    ALTER COLUMN name TYPE VARCHAR(255);
