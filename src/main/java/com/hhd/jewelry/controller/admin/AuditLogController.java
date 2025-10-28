package com.hhd.jewelry.controller.admin;

import com.hhd.jewelry.entity.AuditLog;
import com.hhd.jewelry.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/audits")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditService;

    @GetMapping
    public String viewAuditLogs(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            Model model
    ) {
        // ✅ Sắp xếp theo cột "time" vì entity AuditLog có field này
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "time"));
        Page<AuditLog> auditPage;

        if (startDate != null && endDate != null) {
            auditPage = auditService.findByDateRange(
                    startDate.atStartOfDay(),
                    endDate.plusDays(1).atStartOfDay(),
                    pageable
            );
        } else {
            auditPage = auditService.getAllLogs(pageable);
        }

        model.addAttribute("audits", auditPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", auditPage.getTotalPages());
        model.addAttribute("totalItems", auditPage.getTotalElements());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "admin/audits/audit";
    }
}
