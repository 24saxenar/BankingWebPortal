# 🏦 Enterprise Banking Portal - Design Patterns Implementation

A full-stack, enterprise-grade Spring Boot web application showcasing **13 Software Design Patterns** across Creational, Structural, Behavioral, and Architectural paradigms.

---

## 🚀 Tech Stack
- **Framework:** Spring Boot 2.7.18
- **Language:** Java 11
- **UI & Frontend:** Thymeleaf, HTML5, Bootstrap 5
- **ORM & Database:** Spring Data JPA, H2 In-Memory Database
- **Build Tool:** Maven

---

## 📐 13 Design Patterns Implemented

| # | Pattern | Category | Implementation Component | Purpose in Banking Application |
|---|---|---|---|---|
| 1 | **Singleton** | Creational | `PortalMediator`, Spring Context | Ensures centralized coordination of services and state |
| 2 | **Abstract Factory** | Creational | `LoanFactory`, `SalariedFactory`, `SelfEmployedFactory` | Produces product families for Salaried vs Self-Employed loans |
| 3 | **Builder** | Creational | `LoanApplicationBuilder` | Dynamically constructs custom loan applications step-by-step |
| 4 | **Prototype** | Creational | `StandardLoanOffer` (`Cloneable`) | Clones pre-approved festive loan packages for 1-click fast apply |
| 5 | **Bridge** | Structural | `NotificationChannel`, `EmailNotification` | Decouples notification channels from loan approval workflows |
| 6 | **Proxy** | Structural | `AuthProxy` | Intercepts logins, authenticates against DB, and enforces role access |
| 7 | **Observer** | Behavioral | `AuditDepartment`, `AccountsDepartment` | Notifies compliance audit and funds disbursement on approval |
| 8 | **Chain of Responsibility** | Behavioral | `Approver`, `BranchManager`, `RegionalHead`, `RiskCommittee` | Escalates approval requests based on loan limit thresholds |
| 9 | **Mediator** | Behavioral | `PortalMediator` | Central hub managing communication between repo, chain, observers, and strategies |
| 10 | **Template Method** | Behavioral | `ApplicationScorer`, `SalariedScorer`, `SelfEmployedScorer` | Rigid underwriting skeleton (Identity Verification $\rightarrow$ Criteria Check $\rightarrow$ Risk Scoring) |
| 11 | **Strategy** | Behavioral | `InterestCalculationStrategy` (`Standard`, `Privilege`, `Festive`) | Interchangeable pricing algorithms calculating dynamic interest rates and monthly EMI |
| 12 | **Memento** | Behavioral | `LoanMemento`, `LoanCaretaker` | Captures state snapshots allowing rollback/undo for approvals and rejections |
| 13 | **MVC** | Architectural | `LoanApplication` (Model), `login/customer/employee.html` (View), `WebController` (Controller) | Clean separation of business data, presentation templates, and HTTP routing |

---

## 🏃 Running the Application

### Prerequisites
- JDK 11 installed
- IntelliJ IDEA or Maven

### Run via IntelliJ IDEA
1. Open the project folder in IntelliJ IDEA.
2. Ensure Project SDK is set to Java 11 (**File** $\rightarrow$ **Project Structure** $\rightarrow$ **SDK**).
3. Open `src/main/java/com/bank/BankingApplication.java` and click the **Run ▶️** button.
4. Navigate to `http://localhost:8081` in your browser.

---

## 🔑 Demo Test Accounts

| Role | Username | Password | Access Details |
|---|---|---|---|
| **Customer** | `cust1` | `pass` | Access to Custom Loan Builder, Criteria Input, and 1-Click Festive Apply |
| **Branch Manager** | `emp1` | `pass` | Access to Loan Queue, Approval/Escalation, Rejection, and Memento Undo |
