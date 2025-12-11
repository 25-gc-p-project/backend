package com.hyodream.backend.product.dto;

import com.hyodream.backend.product.domain.ReviewRating;
import com.hyodream.backend.product.domain.ReviewSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ReviewRequestDto {
    @Schema(description = "리뷰할 상품 ID (내부 DB ID)", example = "100")
    private Long productId;

    @Schema(description = "리뷰 내용", example = "배송도 빠르고 부모님이 좋아하십니다.")
    private String content;

    @Schema(description = "평점 (1~5점)", example = "5")
    private int score;

    @Schema(description = "만족도 평가 (GOOD, AVERAGE, BAD) - score 입력 시 자동 계산됨", example = "GOOD")
    private ReviewRating rating;

    // --- [New] 외부 리뷰 연동용 필드 ---
    @Schema(description = "리뷰 출처 (HYODREAM, NAVER)", hidden = true)
    private ReviewSource source;

    @Schema(description = "외부 리뷰 ID (중복 방지용)", hidden = true)
    private String externalReviewId;

    @Schema(description = "작성자 이름 (마스킹됨)", hidden = true)
    private String authorName;

    @Schema(description = "구매 옵션", hidden = true)
    private String productOption;

    @Schema(description = "첨부 이미지 URL 리스트", hidden = true)
    private List<String> images;
}
