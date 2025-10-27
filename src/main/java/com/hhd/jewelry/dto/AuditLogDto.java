package com.hhd.jewelry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuditLogDto {
    private Integer id;
    private String adminName;
    private String role;
    private String action;
    private String details;
    private LocalDateTime time;
}
