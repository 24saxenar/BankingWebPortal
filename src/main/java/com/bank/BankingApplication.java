package com.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Component;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import javax.annotation.PostConstruct;
import javax.persistence.*;
import java.util.*;

@SpringBootApplication
public class BankingApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingApplication.class, args);
        System.out.println("\n\n=======================================================");
        System.out.println("WEB PORTAL IS RUNNING! Go to http://localhost:8081 in Chrome");
        System.out.println("=======================================================\n\n");
    }
}

// =========================================================================
// 1. MODEL LAYER (MVC Pattern) & DOMAIN ENTITIES
// =========================================================================

@Entity
class LoanApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String customerName;
    public String pan;
    public String loanType;
    public double amount;
    public int creditScore = 750;      // Customer entered criteria
    public double monthlyIncome = 60000; // Customer entered criteria
    public int score;                  // System evaluated score (Template Method)
    public String status = "PENDING";  // PENDING, APPROVED, REJECTED
    public String interestStrategy = "Standard";
    public double interestRate = 10.5;
    public double estimatedEmi = 0.0;

    // Getters for Thymeleaf UI
    public Long getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getLoanType() { return loanType; }
    public double getAmount() { return amount; }
    public int getCreditScore() { return creditScore; }
    public double getMonthlyIncome() { return monthlyIncome; }
    public int getScore() { return score; }
    public String getStatus() { return status; }
    public String getInterestStrategy() { return interestStrategy; }
    public double getInterestRate() { return interestRate; }
    public double getEstimatedEmi() { return estimatedEmi; }

    // --- PATTERN: MEMENTO (Originator capabilities) ---
    public LoanMemento createMemento() {
        return new LoanMemento(this.id, this.status, this.amount);
    }

    public void restoreFromMemento(LoanMemento memento) {
        this.status = memento.getStatus();
        this.amount = memento.getAmount();
    }
}

@Entity
class UserAccount {
    @Id
    public String username;
    public String password;
    public String role;
    public UserAccount() {}
    public UserAccount(String u, String p, String r) { username=u; password=p; role=r; }
}

interface LoanRepository extends JpaRepository<LoanApplication, Long> {
    List<LoanApplication> findByStatus(String status);
    List<LoanApplication> findByStatusNot(String status);
}

interface UserRepository extends JpaRepository<UserAccount, String> {}

// --- PATTERN: MVC (Presentation Model / ViewModel) ---
class LoanViewModel {
    private final Long id;
    private final String customerName;
    private final String loanType;
    private final String formattedAmount;
    private final double amount;
    private final int creditScore;
    private final String formattedIncome;
    private final int score;
    private final String status;
    private final String strategyName;
    private final double interestRate;
    private final String formattedEmi;

    public LoanViewModel(LoanApplication app) {
        this.id = app.getId();
        this.customerName = app.getCustomerName();
        this.loanType = app.getLoanType();
        this.amount = app.getAmount();
        this.formattedAmount = String.format("₹%,.2f", app.getAmount());
        this.creditScore = app.getCreditScore();
        this.formattedIncome = String.format("₹%,.2f", app.getMonthlyIncome());
        this.score = app.getScore();
        this.status = app.getStatus();
        this.strategyName = app.getInterestStrategy();
        this.interestRate = app.getInterestRate();
        this.formattedEmi = String.format("₹%,.2f", app.getEstimatedEmi());
    }

    public Long getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getLoanType() { return loanType; }
    public String getFormattedAmount() { return formattedAmount; }
    public double getAmount() { return amount; }
    public int getCreditScore() { return creditScore; }
    public String getFormattedIncome() { return formattedIncome; }
    public int getScore() { return score; }
    public String getStatus() { return status; }
    public String getStrategyName() { return strategyName; }
    public double getInterestRate() { return interestRate; }
    public String getFormattedEmi() { return formattedEmi; }
}

// =========================================================================
// 2. DESIGN PATTERNS IMPLEMENTATION
// =========================================================================

// --- PATTERN: MEMENTO (State Snapshot & Caretaker) ---
class LoanMemento {
    private final Long loanId;
    private final String status;
    private final double amount;
    private final long timestamp;

    public LoanMemento(Long loanId, String status, double amount) {
        this.loanId = loanId;
        this.status = status;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }

    public Long getLoanId() { return loanId; }
    public String getStatus() { return status; }
    public double getAmount() { return amount; }
    public long getTimestamp() { return timestamp; }
}

@Service
class LoanCaretaker {
    private final Deque<LoanMemento> history = new ArrayDeque<>();

    public void saveSnapshot(LoanApplication app) {
        history.push(app.createMemento());
        System.out.println("[Memento Caretaker] Saved state snapshot for Loan #" + app.getId() + " [Status: " + app.getStatus() + "]");
    }

    public LoanMemento undoLast() {
        if (!history.isEmpty()) {
            LoanMemento m = history.pop();
            System.out.println("[Memento Caretaker] Restoring state for Loan #" + m.getLoanId() + " -> [Status: " + m.getStatus() + "]");
            return m;
        }
        return null;
    }

    public boolean hasHistory() {
        return !history.isEmpty();
    }
}

// --- PATTERN: STRATEGY (Interchangeable Interest & EMI Algorithms) ---
interface InterestCalculationStrategy {
    String getStrategyName();
    double calculateAnnualRate(int creditScore);
    double calculateMonthlyEmi(double principal, double annualRate, int tenureMonths);
}

@Component("standardStrategy")
class StandardFixedStrategy implements InterestCalculationStrategy {
    public String getStrategyName() { return "Standard Fixed (10.5%)"; }
    public double calculateAnnualRate(int creditScore) { return 10.5; }
    public double calculateMonthlyEmi(double principal, double annualRate, int tenureMonths) {
        double r = (annualRate / 12) / 100;
        return Math.round((principal * r * Math.pow(1 + r, tenureMonths)) / (Math.pow(1 + r, tenureMonths) - 1));
    }
}

@Component("privilegeStrategy")
class PrivilegeReducingStrategy implements InterestCalculationStrategy {
    public String getStrategyName() { return "Privilege Reducing (8.5% with credit bonus)"; }
    public double calculateAnnualRate(int creditScore) {
        return creditScore >= 750 ? 7.9 : 8.5;
    }
    public double calculateMonthlyEmi(double principal, double annualRate, int tenureMonths) {
        double r = (annualRate / 12) / 100;
        return Math.round((principal * r * Math.pow(1 + r, tenureMonths)) / (Math.pow(1 + r, tenureMonths) - 1));
    }
}

@Component("festiveStrategy")
class FestivePromoStrategy implements InterestCalculationStrategy {
    public String getStrategyName() { return "Festive Promo (7.5% Promotional)"; }
    public double calculateAnnualRate(int creditScore) { return 7.5; }
    public double calculateMonthlyEmi(double principal, double annualRate, int tenureMonths) {
        double r = (annualRate / 12) / 100;
        return Math.round((principal * r * Math.pow(1 + r, tenureMonths)) / (Math.pow(1 + r, tenureMonths) - 1));
    }
}

@Service
class StrategyContext {
    @Autowired private List<InterestCalculationStrategy> strategyList;

    public InterestCalculationStrategy resolveStrategy(String strategyName) {
        if (strategyName != null) {
            for (InterestCalculationStrategy s : strategyList) {
                if (s.getClass().getSimpleName().toLowerCase().contains(strategyName.toLowerCase())) {
                    return s;
                }
            }
        }
        return strategyList.isEmpty() ? new StandardFixedStrategy() : strategyList.get(0);
    }
}

// --- PATTERN: SINGLETON (Spring Beans) & MEDIATOR ---
@Service
class PortalMediator {
    @Autowired private LoanRepository loanRepo;
    @Autowired private NotificationChannel notificationChannel;
    @Autowired private AuditDepartment auditObserver;
    @Autowired private AccountsDepartment accountsObserver;
    @Autowired private StrategyContext strategyContext;
    @Autowired private LoanCaretaker caretaker;
    
    private Approver approvalChain;

    @PostConstruct
    public void init() {
        Approver bm = new BranchManager();
        Approver rh = new RegionalHead();
        Approver rc = new RiskCommittee();
        bm.setNext(rh); rh.setNext(rc);
        this.approvalChain = bm;
    }

    public void submitApplication(LoanApplication app) {
        // Abstract Factory Pattern
        LoanFactory factory = "Salaried".equalsIgnoreCase(app.loanType) ? new SalariedFactory() : new SelfEmployedFactory();
        LoanProduct product = factory.createLoan();
        
        // Template Method Pattern (Validates entered criteria & calculates risk score)
        ApplicationScorer scorer = product instanceof SalariedPersonalLoan ? new SalariedScorer() : new SelfEmployedScorer();
        app.score = scorer.evaluate(app);
        
        // Strategy Pattern for Interest Rate and EMI Calculation
        InterestCalculationStrategy strategy = strategyContext.resolveStrategy(app.interestStrategy);
        app.interestRate = strategy.calculateAnnualRate(app.score);
        app.estimatedEmi = strategy.calculateMonthlyEmi(app.amount, app.interestRate, 24); // 24-month tenure
        
        loanRepo.save(app);
        notificationChannel.send("App received for " + app.customerName + " [CIBIL: " + app.creditScore + ", Strategy: " + strategy.getStrategyName() + "]");
    }

    public void requestApproval(Long loanId, double amount, String approverRole) {
        // Chain of Responsibility Pattern
        approvalChain.processApproval(loanId, amount, this);
    }

    public void executeApproval(Long loanId, String approverTitle) {
        LoanApplication app = loanRepo.findById(loanId).orElse(null);
        if (app != null) {
            // MEMENTO PATTERN: Save state snapshot before mutation
            caretaker.saveSnapshot(app);

            app.status = "APPROVED";
            loanRepo.save(app);
            String msg = "Loan #" + loanId + " Approved by " + approverTitle;
            notificationChannel.send(msg);
            auditObserver.update(msg);
            accountsObserver.update(msg);
        }
    }

    public void executeRejection(Long loanId, String approverTitle) {
        LoanApplication app = loanRepo.findById(loanId).orElse(null);
        if (app != null) {
            // MEMENTO PATTERN: Save state snapshot before rejection
            caretaker.saveSnapshot(app);

            app.status = "REJECTED";
            loanRepo.save(app);
            String msg = "Loan #" + loanId + " REJECTED by " + approverTitle;
            notificationChannel.send(msg);
            auditObserver.update(msg);
        }
    }

    public LoanApplication undoLastAction() {
        // MEMENTO PATTERN: Restore previous state snapshot (works for both Approve & Reject)
        LoanMemento memento = caretaker.undoLast();
        if (memento != null) {
            LoanApplication app = loanRepo.findById(memento.getLoanId()).orElse(null);
            if (app != null) {
                app.restoreFromMemento(memento);
                loanRepo.save(app);
                String msg = "ROLLBACK: Loan #" + app.id + " status reverted to " + app.status + " via Memento";
                notificationChannel.send(msg);
                auditObserver.update(msg);
                return app;
            }
        }
        return null;
    }

    public boolean canUndo() {
        return caretaker.hasHistory();
    }
}

// --- PATTERN: OBSERVER ---
@Component
class AuditDepartment { public void update(String event) { System.out.println("[Audit] " + event); } }

@Component
class AccountsDepartment { public void update(String event) { System.out.println("[Accounts] " + event); } }

// --- PATTERN: BRIDGE ---
interface NotificationChannel { void send(String msg); }
@Component
class EmailNotification implements NotificationChannel {
    public void send(String msg) { System.out.println("[Email Sent] " + msg); }
}

// --- PATTERN: ABSTRACT FACTORY ---
abstract class LoanProduct {}
class SalariedPersonalLoan extends LoanProduct {}
class SelfEmployedPersonalLoan extends LoanProduct {}
interface LoanFactory { LoanProduct createLoan(); }
class SalariedFactory implements LoanFactory { public LoanProduct createLoan() { return new SalariedPersonalLoan(); } }
class SelfEmployedFactory implements LoanFactory { public LoanProduct createLoan() { return new SelfEmployedPersonalLoan(); } }

// --- PATTERN: BUILDER ---
class LoanApplicationBuilder {
    private LoanApplication app = new LoanApplication();
    public LoanApplicationBuilder setName(String n) { app.customerName = n; return this; }
    public LoanApplicationBuilder setPan(String p) { app.pan = p; return this; }
    public LoanApplicationBuilder setType(String t) { app.loanType = t; return this; }
    public LoanApplicationBuilder setAmount(double a) { app.amount = a; return this; }
    public LoanApplicationBuilder setCreditScore(int s) { app.creditScore = s; return this; }
    public LoanApplicationBuilder setMonthlyIncome(double inc) { app.monthlyIncome = inc; return this; }
    public LoanApplicationBuilder setStrategy(String s) { app.interestStrategy = s; return this; }
    public LoanApplication build() { return app; }
}

// --- PATTERN: PROTOTYPE ---
class StandardLoanOffer implements Cloneable {
    public String name; public double defaultAmount; public String defaultType; public String defaultStrategy;
    public int defaultScore; public double defaultIncome;
    public StandardLoanOffer(String n, double a, String t, String s, int score, double income) { 
        name=n; defaultAmount=a; defaultType=t; defaultStrategy=s; defaultScore=score; defaultIncome=income;
    }
    public StandardLoanOffer clone() {
        try { return (StandardLoanOffer) super.clone(); } catch (CloneNotSupportedException e) { return null; }
    }
}

// --- PATTERN: TEMPLATE METHOD (Criteria Validation & Risk Scoring) ---
abstract class ApplicationScorer {
    // Skeleton algorithm: 1. Verify -> 2. Check Criteria -> 3. Calculate Risk Score
    public final int evaluate(LoanApplication app) {
        verifyIdentity(app);
        validateEligibilityCriteria(app);
        return calculateRiskScore(app);
    }

    private void verifyIdentity(LoanApplication app) {
        System.out.println("System: Verifying PAN " + app.pan);
    }

    // Criteria enforcement step
    protected void validateEligibilityCriteria(LoanApplication app) {
        if (app.creditScore < 600) {
            throw new IllegalArgumentException("Eligibility check failed: Minimum CIBIL score of 600 required to apply (Entered: " + app.creditScore + ").");
        }
        if ("Privilege".equalsIgnoreCase(app.interestStrategy)) {
            if (app.creditScore < 750) {
                throw new IllegalArgumentException("Privilege Loan Criteria Not Met: Requires CIBIL score >= 750 (Entered: " + app.creditScore + ").");
            }
            if (app.monthlyIncome < 50000) {
                throw new IllegalArgumentException("Privilege Loan Criteria Not Met: Requires Monthly Income >= ₹50,000 (Entered: ₹" + app.monthlyIncome + ").");
            }
        }
    }

    protected abstract int calculateRiskScore(LoanApplication app);
}

class SalariedScorer extends ApplicationScorer {
    protected int calculateRiskScore(LoanApplication app) {
        int score = app.creditScore;
        if (app.monthlyIncome >= 75000) score += 20; // Income stability bonus
        return Math.min(score, 900);
    }
}

class SelfEmployedScorer extends ApplicationScorer {
    protected int calculateRiskScore(LoanApplication app) {
        int score = (int) (app.creditScore * 0.95); // Higher volatility scrutiny
        if (app.monthlyIncome >= 100000) score += 25;
        return Math.min(score, 900);
    }
}

// --- PATTERN: CHAIN OF RESPONSIBILITY ---
abstract class Approver {
    protected String title; protected double limit; protected Approver next;
    public Approver(String t, double l) { title=t; limit=l; }
    public void setNext(Approver n) { next = n; }
    public void processApproval(Long loanId, double amount, PortalMediator mediator) {
        if (amount <= limit) {
            System.out.println("[" + title + "] Approved within limit.");
            mediator.executeApproval(loanId, title);
        } else if (next != null) {
            System.out.println("[" + title + "] Amount too high. Escalating...");
            next.processApproval(loanId, amount, mediator);
        }
    }
}
class BranchManager extends Approver { public BranchManager() { super("Branch Manager", 50000); } }
class RegionalHead extends Approver { public RegionalHead() { super("Regional Head", 200000); } }
class RiskCommittee extends Approver { public RiskCommittee() { super("Risk Committee", 5000000); } }

// --- PATTERN: PROXY (Authentication Gateway) ---
@Service
class AuthProxy {
    @Autowired private UserRepository userRepo;
    
    @PostConstruct
    public void seedUsers() {
        if(userRepo.count() == 0) {
            userRepo.save(new UserAccount("cust1", "pass", "CUSTOMER"));
            userRepo.save(new UserAccount("emp1", "pass", "BRANCH_MANAGER"));
            userRepo.save(new UserAccount("emp2", "pass", "REGIONAL_HEAD"));
            userRepo.save(new UserAccount("emp3", "pass", "RISK_COMMITTEE"));
        }
    }
    
    public String authenticate(String user, String pass) {
        UserAccount acc = userRepo.findById(user).orElse(null);
        if (acc != null && acc.password.equals(pass)) return acc.role;
        return null;
    }
}
