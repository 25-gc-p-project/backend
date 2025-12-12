package com.hyodream.backend.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationRequestDto {

    @JsonProperty("diseases")
    private List<String> diseaseNames;

    @JsonProperty("allergies")
    private List<String> allergyNames;

    @JsonProperty("goals")
    private List<String> healthGoalNames;

    @JsonProperty("candidates")
    private List<CandidateProductDto> candidates;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandidateProductDto {
        private Long id;
        private String name;
        private List<String> benefits;
        private List<String> allergens;
        private String category;
    }
}
