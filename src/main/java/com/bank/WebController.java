package com.bank;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

// =========================================================================
// CONTROLLER LAYER (MVC Pattern)
// Dispatches incoming web actions, mutates models, and selects views.
// =========================================================================
@Controller
public class WebController {

    @Autowired private AuthProxy authProxy;
    @Autowired private PortalMediator mediator;
    @Autowired private LoanRepository loanRepo;

    @GetMapping("/")
    public String loginPage(Model model) {
        return "login"; // View: login.html
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session, Model model) {
        // Pattern: Proxy Authentication
        String role = authProxy.authenticate(username, password);
        if (role != null) {
            session.setAttribute("user", username);
            session.setAttribute("role", role);
            if (role.equals("CUSTOMER")) return "redirect:/customer";
            return "redirect:/employee";
        }
        model.addAttribute("error", "Invalid Credentials (Try cust1/pass or emp1/pass)");
        return "login";
    }

    @GetMapping("/customer")
    public String customerDashboard(HttpSession session, Model model) {
        if (session.getAttribute("user") == null) return "redirect:/";
        model.addAttribute("username", session.getAttribute("user"));
        return "customer"; // View: customer.html
    }

    @PostMapping("/customer/apply")
    public String applyCustom(@RequestParam double amount, 
                              @RequestParam String type, 
                              @RequestParam(defaultValue = "Standard") String strategy,
                              @RequestParam(defaultValue = "750") int creditScore,
                              @RequestParam(defaultValue = "60000") double monthlyIncome,
                              HttpSession session) {
        try {
            // Pattern: Builder Pattern with dynamic Criteria & Strategy selection
            LoanApplication app = new LoanApplicationBuilder()
                    .setName((String) session.getAttribute("user"))
                    .setPan("USERPAN123")
                    .setType(type)
                    .setAmount(amount)
                    .setCreditScore(creditScore)
                    .setMonthlyIncome(monthlyIncome)
                    .setStrategy(strategy)
                    .build();

            // Template Method pattern inside mediator validates criteria & computes score
            mediator.submitApplication(app);
            return "redirect:/customer?success=true";
        } catch (IllegalArgumentException ex) {
            // MVC: Return user-friendly validation error from Template Method
            String encodedErr = URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/customer?error=" + encodedErr;
        }
    }

    @PostMapping("/customer/quick-apply")
    public String applyQuick(HttpSession session) {
        // Pattern: Prototype Pattern (Clone predefined promotional offer)
        StandardLoanOffer festiveOffer = new StandardLoanOffer("Diwali Special", 45000, "Salaried", "Festive", 780, 85000);
        StandardLoanOffer clone = festiveOffer.clone();
        
        LoanApplication app = new LoanApplicationBuilder()
                .setName((String) session.getAttribute("user"))
                .setPan("USERPAN123")
                .setType(clone.defaultType)
                .setAmount(clone.defaultAmount)
                .setStrategy(clone.defaultStrategy)
                .setCreditScore(clone.defaultScore)
                .setMonthlyIncome(clone.defaultIncome)
                .build();

        mediator.submitApplication(app);
        return "redirect:/customer?success=true";
    }

    @GetMapping("/employee")
    public String employeeDashboard(HttpSession session, Model model) {
        if (session.getAttribute("user") == null) return "redirect:/";
        
        // MVC Model Population: Transforming JPA domain entities into presentation ViewModels
        List<LoanViewModel> pendingLoans = loanRepo.findByStatus("PENDING")
                .stream()
                .map(LoanViewModel::new)
                .collect(Collectors.toList());

        List<LoanViewModel> processedLoans = loanRepo.findByStatusNot("PENDING")
                .stream()
                .map(LoanViewModel::new)
                .collect(Collectors.toList());

        model.addAttribute("username", session.getAttribute("user"));
        model.addAttribute("role", session.getAttribute("role"));
        model.addAttribute("loans", pendingLoans);
        model.addAttribute("processedLoans", processedLoans);
        model.addAttribute("canUndo", mediator.canUndo());

        return "employee"; // View: employee.html
    }

    @PostMapping("/employee/approve")
    public String approveLoan(@RequestParam Long loanId, @RequestParam double amount, HttpSession session) {
        String role = (String) session.getAttribute("role");
        mediator.requestApproval(loanId, amount, role);
        return "redirect:/employee?approved=true";
    }

    @PostMapping("/employee/reject")
    public String rejectLoan(@RequestParam Long loanId, HttpSession session) {
        String role = (String) session.getAttribute("role");
        mediator.executeRejection(loanId, role != null ? role : "Officer");
        return "redirect:/employee?rejected=true";
    }

    @PostMapping("/employee/undo")
    public String undoAction(HttpSession session) {
        // Pattern: Memento Pattern rollback (works for both Approve and Reject)
        LoanApplication restored = mediator.undoLastAction();
        if (restored != null) {
            return "redirect:/employee?undone=true";
        }
        return "redirect:/employee?error=No+actions+to+undo";
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
