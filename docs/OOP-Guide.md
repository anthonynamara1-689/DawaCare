# DawaCare: Building a Medication Adherence System Step by Step — An OOP Guide

Oct 10, 2026 · @Namara Anthony

## How to use this guide

DawaCare is a Java desktop system that turns a doctor's prescription into a daily dose schedule and measures how well the patient follows it. This guide retells how it was built in eleven steps. At every step it names the OOP2 concepts used, shows where they live in the code, and explains why they were the right tool.

You can read it three ways:

- **As a build story.** Steps 1 to 11 follow the order the project was actually built, from redesigning the database to publishing on GitHub.
- **As an OOP textbook.** Every step ends with an *OOP concepts in this step* table. The *Deep dive* section then collects the four pillars and the other concepts in one place, each with real code from DawaCare.
- **As presentation notes.** The last sections give a demo script, the bugs we hit (and what they teach), and answers to likely examiner questions.

### What DawaCare does

| Role | What they can do |
| --- | --- |
| Patient | Choose a doctor, approve caregivers, see today's doses, mark them taken or skipped, see adherence, get reminders, export a PDF report |
| Doctor | Registers with a license number, waits for admin approval, writes prescriptions for linked patients, views each patient's adherence |
| Caregiver | Requests access to a patient (the patient must approve), records doses for them, gets missed-dose alerts |
| Administrator | Approves doctors, activates or deactivates accounts, manages the medication catalogue, reads the audit log |

The heart of the system is one loop: **doctor prescribes → system generates every dose → patient or caregiver records each dose → adherence is calculated → everyone sees it.** Adherence = doses taken ÷ (taken + missed + skipped).

**Technology:** Java 25, Swing (NetBeans GUI Builder), MySQL 8.4 through JDBC, Maven, BCrypt for passwords, OpenPDF for reports.

**About the concept list:** OOP2 courses differ slightly. This guide covers the usual OOP2 topics: classes and objects, the four pillars, interfaces, exceptions, generics and collections, enums and records, nested and anonymous classes, lambdas, GUI and event handling, multithreading, JDBC and design patterns. Check it against your own course outline and tell me if a topic is missing.

## Quick reference: OOP2 concepts in DawaCare

Every major OOP2 concept appears somewhere in DawaCare; this table tells you where to point when an examiner asks. The step column tells you where in this guide it is explained.

| Concept | One-line meaning | Best example in DawaCare | Step |
| --- | --- | --- | --- |
| Class and object | A class is a blueprint; an object is one thing built from it | `User` class; one `User` object per logged-in person | 3 |
| Encapsulation | Hide data inside a class; expose only safe methods | `User` has private fields and getters only; `Session` hides who is logged in | 3 |
| Abstraction | Show what something does, hide how | `AuthService.login()` hides SQL, hashing and status checks from the UI | 3, 7 |
| Inheritance | A class reuses and extends another ("is-a") | `PrescriptionDao extends BaseDao`; `ServiceException extends Exception`; forms extend `JFrame` | 4, 7 |
| Abstract class | A class that cannot be created on its own, only extended | `BaseDao` | 7 |
| Polymorphism: overriding | A subclass replaces a parent's method | `toString()` in `DoctorOption`; `isCellEditable()` in the table model; `doInBackground()` in `SwingWorker` | 5, 6 |
| Polymorphism: overloading | Same method name, different parameters | `BaseDao.update(String, …)` vs `update(Connection, String, …)` | 7 |
| Dynamic dispatch | The real object type decides which method runs | `JFrame dashboard = new DoctorDashboard(); dashboard.setVisible(true);` | 3 |
| Interface | A contract of methods without a body | `ServiceCall`, `ServiceAction`, `SqlCall`; Swing listeners | 6 |
| Functional interface + lambda | A one-method interface you can fill with a short function | `UiHelper.run(this, () -> careService.approveCaregiver(id), this::refreshCaregivers)` | 6 |
| Enum | A fixed set of named constants | `Role`, `UserStatus` | 3 |
| Record | A short immutable data class | `RegistrationRequest`, `DoseItem`, `AdherenceStats` | 4 |
| Custom exception | Your own exception type with a friendly message | `ServiceException`, `AuthException` | 4 |
| Exception handling | `try`, `catch`, `finally`, try-with-resources, translation | `SQLException` turned into `ServiceException`; `rollback()` in `catch` | 4 |
| Generics | Classes and methods that work for any type, safely | `<T> T db(SqlCall<T> call, …)`; `List<DoseItem>`; `SwingWorker<User, Void>` | 6 |
| Collections and streams | Lists, sets, and processing them | `TreeSet<LocalTime>` sorts dose times; `stream().filter().findFirst()` | 7 |
| Static members and utility classes | Belongs to the class, not an object | `Session`, `UiHelper`, `DoseGenerator`, `PasswordPolicy` | 3, 6 |
| Final and immutability | Cannot be changed after creation | `private final` services and models; records | 3 |
| Nested and anonymous classes | A class inside another, or one with no name | Anonymous `SwingWorker` and `DefaultTableModel` subclasses | 5 |
| Packages and access modifiers | Group classes; control who can see what | `ui`, `service`, `dao`, `model`; `private` / `protected` / `public` | 2 |
| Composition | A class *has* another object instead of *being* one | `DoctorDashboard` has a `DoseAdherencePanel` and a `PrescriptionService` | 8 |
| GUI and event handling | Components that react to user actions | Swing forms, `ActionListener`, `CardLayout`, `JTabbedPane` | 4, 5 |
| Multithreading | Several tasks at once | `SwingWorker`, `ScheduledExecutorService` in `MissedDoseJob`, `javax.swing.Timer` | 5, 8 |
| JDBC | Java talking to a database | `PreparedStatement`, transactions, batch inserts | 2, 4, 7 |
| File I/O | Reading and writing files | Loading `db.properties`; writing the PDF report | 2, 10 |
| Design patterns | Proven solutions to common problems | DAO, layered architecture, utility class, factory-style dashboard choice, observer (listeners) | 3 |

## The architecture: layers and packages

DawaCare is built in layers: the UI only talks to services, services only talk to DAOs, and only DAOs talk to MySQL. Each layer is a package, and each package has one job.

&#91;embedded content: DawaCare architecture · 4 layers, 4 supporting packages\]

The service layer (highlighted) is the gatekeeper: every access check and business rule lives there, so no screen can bypass it.

### Following one click through the layers: *Mark Taken*

1. **ui:** the patient selects a dose in `DoseAdherencePanel` and clicks *Mark Taken*. The button's listener calls `UiHelper.run(…)`, which starts a background `SwingWorker`.
2. **service:** on the background thread, `DoseService.markTaken()` calls `resolvePatient()` to check who may record this dose, then works out the allowed time window.
3. **dao:** `DoseDao.markDose()` sends an `UPDATE` with `?` placeholders through a `PreparedStatement`, using a connection from `DBConnection`.
4. **MySQL** updates the row in `dose_records`.
5. **Back in ui:** `SwingWorker.done()` runs on the Event Dispatch Thread and refreshes the table and the adherence figures.

If any step fails, a `ServiceException` with a friendly message travels back up and appears in a dialog.

## Step 1: Requirements and the database redesign

Before writing any Java, we reviewed the first database design and fixed eight problems, because every class we would write later depends on this structure.

### What was wrong and how we fixed it

| Problem in the first design | Why it mattered | Fix |
| --- | --- | --- |
| Anyone could register as a doctor | A stranger could write prescriptions | Doctors start as `PENDING`; an admin verifies the license |
| Caregivers could attach themselves to any patient | Private medical data exposed | `caregiver_patients.status`: `PENDING` until the patient approves |
| No doctor–patient link | A doctor could see everyone, or no one | New `doctor_patients` table |
| Nothing created the dose records | The core workflow had a gap | Doses are generated when a prescription is saved |
| `patient_id` stored twice, dates stored twice | The copies could disagree | Store each fact once; add views (`v_dose_details`) for easy queries |
| Adherence percentage stored in a table | Wrong the moment a dose is marked | Calculated on demand by a query |
| A `reminders` table with status `SENT` | A desktop app cannot send anything | Replaced by a general `notifications` table |
| `ON DELETE CASCADE` on medical data | Deleting one user wiped years of records | `RESTRICT` plus soft delete (`status = INACTIVE`) |

### How this connects to OOP

A good database design and a good class design follow the same rules, so this step already practised OOP thinking:

- **Modelling the real world (abstraction).** Each table describes one real thing: a user, a prescription, a dose. Later each became a Java class or record with the same fields.
- **Relationships become object associations.** One prescription *has many* items; one item *has many* doses. A many-to-many relationship (caregivers and patients) became its own "link" table, just as in OOP it becomes its own class.
- **Single source of truth (like encapsulation).** Each fact lives in one place and is changed in one place. Encapsulation applies the same idea to objects.
- **Fixed value sets.** Columns such as `role ENUM('ADMIN','DOCTOR','PATIENT','CAREGIVER')` later became Java `enum` types, so the database and the code agree on the allowed values.

| OOP concept in this step | Where |
| --- | --- |
| Abstraction (modelling) | Tables for users, prescriptions, items, doses |
| Associations (has-a, one-to-many, many-to-many) | `prescription_items`, `caregiver_patients`, `doctor_patients` |
| Single responsibility | Each table stores one kind of fact |

## Step 2: Project setup and the foundation classes

We created a Maven project, organised it into packages, and wrote two small classes that everything else depends on: `DBConnection` and `PasswordUtil`.

### What we did

1. Created a **Maven** project in NetBeans. Maven downloads the libraries listed in `pom.xml` (MySQL driver, jBCrypt, later OpenPDF), so the project builds the same on any computer.
2. Created the **packages** `model`, `dao`, `service`, `ui`, `util`, `jobs` (and later `report`).
3. Created a dedicated MySQL user, `dawacare`, that can only touch `medical_adherence_db`. The app never logs in as `root`.
4. Moved the database login details into `db.properties` instead of writing them in the code.
5. Wrote and tested `DBConnection` and `PasswordUtil`.

### `DBConnection`: static members and a static initializer

```java
public class DBConnection {
    private static final Properties props = new Properties();

    static {                                   // runs once, when the class is first used
        try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
            props.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load db.properties", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            props.getProperty("db.url"), props.getProperty("db.user"), props.getProperty("db.password"));
    }
}
```

- **`static`** means the field and method belong to the class itself, not to an object. We never write `new DBConnection()`; we call `DBConnection.getConnection()`.
- The **static initializer block** `static { … }` runs exactly once, the first time the class is used, so the settings file is read once.
- **`private`** hides `props` from every other class (encapsulation). Nobody outside can read the password.
- **`final`** means `props` always points to the same `Properties` object.
- **try-with-resources** closes the file stream automatically, even if an error happens.
- Wrapping the error in a `RuntimeException` is **exception translation**: the caller gets a clear message instead of a low-level file error.

### `PasswordUtil`: a utility class

```java
public class PasswordUtil {
    public static String hash(String plain)                 { return BCrypt.hashpw(plain, BCrypt.gensalt(12)); }
    public static boolean verify(String plain, String hash) { return BCrypt.checkpw(plain, hash); }
}
```

A **utility class** groups related static methods. It has no state of its own, so it never needs an object. BCrypt adds a random **salt**, so the same password gives a different hash every time, and `verify` still recognises it.

### Packages and access modifiers

Packages group classes by job, and access modifiers decide who can use what:

| Modifier | Who can see it | Example in DawaCare |
| --- | --- | --- |
| `private` | Only the same class | `User`'s fields; `DBConnection.props` |
| (none) package-private | Classes in the same package | Rarely used here |
| `protected` | Same package and subclasses | `BaseDao.prepare()`, `update()`, `audit()` (Step 7) |
| `public` | Everyone | Service methods the UI calls |

| OOP concept in this step | Where |
| --- | --- |
| Static members and static initializer | `DBConnection` |
| Utility class | `PasswordUtil` |
| Encapsulation (`private`) | `DBConnection.props` |
| Packages and access modifiers | `model`, `dao`, `service`, `ui`, `util` |
| Exceptions: try-with-resources, translation | `DBConnection`'s static block |
| File I/O | Reading `db.properties` |

## Step 3: Login — classes, objects, enums and encapsulation

The login slice introduced the first real classes, and each one has a single job: `User` holds data, `UserDao` reads it, `AuthService` decides, `Session` remembers, and `LoginFrame` shows.

### Enums: `Role` and `UserStatus`

```java
public enum Role { ADMIN, DOCTOR, PATIENT, CAREGIVER }
public enum UserStatus { PENDING, ACTIVE, INACTIVE }
```

An **enum** is a type with a fixed list of values. A typo like `Role.DOCTER` will not compile, whereas a typo in a `String` would only fail at runtime. The values match the MySQL `ENUM` columns, and `Role.valueOf("DOCTOR")` converts a database value into the Java constant.

### `User`: a class with encapsulated data

```java
public class User {
    private int userId;
    private String username;
    private String passwordHash;
    private Role role;
    private UserStatus status;
    // …full name, email, phone

    public User(int userId, String username, String passwordHash, /* … */ Role role, UserStatus status) { … }

    public String getUsername() { return username; }
    public Role getRole()       { return role; }
    // getters only, no setters
}
```

- A **class** is the blueprint; each logged-in person becomes one **object** (`new User(…)`).
- The **constructor** guarantees every `User` is created complete.
- **Encapsulation:** the fields are `private`, and there are getters but **no setters**. Once loaded, a `User` cannot be changed by mistake anywhere in the UI.

### `UserDao`: turning database rows into objects

```java
public User findByUsername(String username) throws SQLException {
    String sql = "SELECT … FROM users WHERE username = ?";
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setString(1, username);                       // the ? is filled safely
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? mapRow(rs) : null;
        }
    }
}
```

DAO means **Data Access Object**: the only class that writes SQL for users. The `?` placeholder in a `PreparedStatement` blocks SQL injection. `mapRow` converts one row into one `User` object.

### `AuthService`: abstraction

`LoginFrame` calls one method, `authService.login(username, password)`. It does not know about SQL, BCrypt or account statuses. That is **abstraction**: the UI knows *what* happens, not *how*. Inside, the service:

1. Rejects blank input.
2. Loads the user and checks the BCrypt hash. It gives the **same message** for an unknown username and a wrong password, so attackers cannot discover which usernames exist.
3. Uses a `switch` on the `UserStatus` enum to block `PENDING` and `INACTIVE` accounts.
4. Starts the session.

### `Session`: static state behind a private constructor

```java
public final class Session {
    private static User currentUser;
    private Session() {}                     // nobody can create a Session object
    public static void start(User user) { currentUser = user; }
    public static User getUser()        { return currentUser; }
    public static boolean hasRole(Role r) { return currentUser != null && currentUser.getRole() == r; }
}
```

There is only ever one logged-in user, so `Session` keeps it in a `static` field. The **private constructor** and `final` keyword stop anyone from creating or extending it. It works like a simplified **Singleton**: one shared piece of state for the whole app.

### `LoginFrame` and polymorphism

`LoginFrame extends JFrame`, so it **inherits** everything a window can do (`setVisible`, `dispose`, `setTitle`). After a successful login, it chooses a dashboard:

```java
JFrame dashboard = switch (user.getRole()) {
    case ADMIN     -> new AdminDashboard();
    case DOCTOR    -> new DoctorDashboard();
    case PATIENT   -> new PatientDashboard();
    case CAREGIVER -> new CaregiverDashboard();
};
dashboard.setVisible(true);
```

This is **polymorphism**. The variable's declared type is `JFrame`, but the actual object is one of four subclasses. The same call, `setVisible(true)`, opens whichever dashboard was created. The `switch` acts as a small **factory**: it decides which class to instantiate.

### Design choice: why `Patient` does not extend `User`

A textbook design might write `class Patient extends User`. We instead kept one `User` class with a `Role` field, and stored role-specific data in separate tables (`patients`, `doctors`, `caregivers`). This mirrors the database, keeps login code identical for all roles, and lets a role's extra data load only when needed. It is **composition over inheritance**: a user *has* a role and a profile, rather than *being* a subclass. Inheritance is still used heavily elsewhere (Steps 4 and 7). This is a good discussion point for examiners.

| OOP concept in this step | Where |
| --- | --- |
| Class, object, constructor | `User` |
| Encapsulation (private fields, getters only) | `User`, `Session` |
| Enum + switch on enum | `Role`, `UserStatus`, `AuthService.login` |
| Abstraction | `AuthService.login()` hides SQL, hashing and rules |
| Inheritance | `LoginFrame extends JFrame` |
| Polymorphism (dynamic dispatch) + factory | Dashboard `switch` in `LoginFrame` |
| Static state, private constructor (Singleton-like) | `Session` |
| DAO pattern | `UserDao` |

## Step 4: Registration — records, custom exceptions and transactions

Registration added three new ideas: a **record** to carry form data, a **custom exception** to report mistakes politely, and a **transaction** so a user is never half-saved.

### `RegistrationRequest`: a record

```java
public record RegistrationRequest(
        Role role, String fullName, String username, String password, String confirmPassword,
        String email, String phone,
        String dateOfBirth, String gender, String address,               // patient fields
        String emergencyContactName, String emergencyContactPhone,
        String licenseNumber, String specialization, String hospitalName) { }
```

A **record** is a short way to write an immutable data class. From that one line, Java generates the private final fields, the constructor, accessor methods (`r.role()`, `r.username()`), and `equals`, `hashCode` and `toString`. The form packs everything into one object and hands it to the service, instead of passing 15 separate arguments. Records are used for almost all DawaCare data from here on.

### `ServiceException`: inheritance for a custom exception

```java
public class ServiceException extends Exception {
    public ServiceException(String message) { super(message); }
}
```

`ServiceException` **inherits** from Java's `Exception`, so it can be thrown and caught like any exception. `super(message)` calls the parent's constructor, which stores the message. Because it extends `Exception` (not `RuntimeException`), it is a **checked exception**: the compiler forces the UI to handle it. The rule we used everywhere: a `ServiceException` message is always safe and friendly to show the user, such as "Passwords do not match."

### `RegistrationService`: validation and exception handling

The service checks every rule before saving: username format, password strength, matching passwords, a valid date of birth for patients, a license number for doctors. It also decides the starting status:

```java
UserStatus status = (r.role() == Role.DOCTOR) ? UserStatus.PENDING : UserStatus.ACTIVE;

try {
    registrationDao.register(r, hash, status);
} catch (SQLIntegrityConstraintViolationException e) {   // more specific type first
    throw new ServiceException(duplicateMessage(e.getMessage()));
} catch (SQLException e) {                                // its parent type second
    throw new ServiceException("Could not create the account. Please try again.");
}
```

- **Catch order matters.** `SQLIntegrityConstraintViolationException` is a *subclass* of `SQLException`, so it must be caught first. If the parent came first, it would catch both, and the specific "username already taken" message would never appear.
- **Exception translation:** a technical database error becomes a friendly `ServiceException`. The UI never sees SQL.
- The regular expressions are `private static final Pattern` constants, compiled once and shared by every call.

### `RegistrationDao`: a transaction

```java
c.setAutoCommit(false);
try {
    int userId = insertUser(c, r, passwordHash, status);
    switch (r.role()) {
        case PATIENT   -> insertPatient(c, userId, r);
        case DOCTOR    -> insertDoctor(c, userId, r);
        case CAREGIVER -> insertCaregiver(c, userId);
        case ADMIN     -> throw new SQLException("Admins cannot self-register");
    }
    c.commit();                 // both rows saved together
} catch (SQLException e) {
    c.rollback();               // or neither is saved
    throw e;
}
```

The `users` row and the profile row are saved **together or not at all**. `RETURN_GENERATED_KEYS` gives back the new `user_id` so the profile row can reference it.

### `RegisterFrame`: GUI with a CardLayout

The form built in the NetBeans GUI Builder uses a **CardLayout** panel: three stacked "cards" (Patient, Doctor, Caregiver), with only one visible at a time. When the user changes the *Register as* combo box, its **event listener** calls `showRoleCard()`, which flips to the matching card.

| OOP concept in this step | Where |
| --- | --- |
| Record (immutable data class) | `RegistrationRequest` |
| Inheritance + `super` | `ServiceException extends Exception` |
| Custom checked exception | `ServiceException` |
| Exception handling: catch order, translation, rethrow | `RegistrationService.register`, `RegistrationDao.register` |
| Static final constants | `USERNAME`, `EMAIL`, `PHONE` patterns |
| Transactions | `RegistrationDao` |
| Event handling + layout managers | `RegisterFrame` (`CardLayout`, combo box listener) |

## Step 5: Admin dashboard — anonymous classes, threads and events

The admin can approve doctors and activate or deactivate accounts; building it taught us to keep the window responsive with background threads, and to customise library classes with anonymous subclasses.

### Security lives in the service, not the screen

Every `AdminService` method starts with `requireAdmin()`. Hiding the admin window from other roles is not enough, because the service is the real gatekeeper:

```java
private User requireAdmin() throws ServiceException {
    User u = Session.getUser();
    if (u == null || u.getRole() != Role.ADMIN)
        throw new ServiceException("Only administrators can do this.");
    return u;
}
```

Business rules are enforced here too: an admin cannot change their own account, and a rejected doctor cannot be re-activated without an approval. Approving a doctor updates two tables and writes an audit entry in one **transaction**.

### Event-driven programming

A GUI program does not run top to bottom. It waits for **events** (a click, a key press) and runs the matching **listener**. NetBeans connects each button to its handler with a method reference:

```java
btnApprove.addActionListener(this::btnApproveActionPerformed);
```

`ActionListener` is an **interface** with one method, `actionPerformed`. `this::btnApproveActionPerformed` is a short way of supplying an object that implements it.

### Multithreading: `SwingWorker` and the Event Dispatch Thread

Swing draws the window and handles clicks on one thread, the **Event Dispatch Thread (EDT)**. If a database query runs on the EDT, the window freezes until it finishes. So database work runs on a **background thread** with `SwingWorker`:

```java
new SwingWorker<Void, Void>() {                 // an anonymous subclass
    private String error;

    @Override
    protected Void doInBackground() {           // runs on a background thread
        try { action.run(); } catch (ServiceException ex) { error = ex.getMessage(); }
        return null;
    }

    @Override
    protected void done() {                     // runs back on the EDT
        if (error != null) JOptionPane.showMessageDialog(AdminDashboard.this, error);
        refreshAll();
    }
}.execute();
```

Rule of thumb: **slow work in `doInBackground()`, screen updates in `done()`.**

### Anonymous classes and overriding

The `new SwingWorker<…>() { … }` above is an **anonymous class**: a subclass with no name, written and used in one place. It **overrides** `doInBackground()` and `done()`. The `@Override` annotation asks the compiler to check that a parent method with that exact signature really exists.

The same trick makes tables read-only:

```java
return new DefaultTableModel(columns, 0) {
    @Override
    public boolean isCellEditable(int row, int col) {
        return false;                           // the admin cannot type into the table
    }
};
```

`DefaultTableModel` normally allows editing. We change one behaviour by overriding one method, and inherit everything else. This is **inheritance plus overriding** in one small block.

### A private nested interface

```java
private interface ServiceAction {
    void run() throws ServiceException;
}

runAction(() -> adminService.approveDoctor(id), "Doctor approved.");
```

`ServiceAction` is an interface **nested** inside `AdminDashboard` and visible only there. The lambda `() -> adminService.approveDoctor(id)` is an object implementing it, so `runAction` can run *any* admin action the same way. In Step 6 this idea moves into a shared class.

| OOP concept in this step | Where |
| --- | --- |
| Authorisation in the service layer | `AdminService.requireAdmin()` |
| Event-driven programming, listeners | Button handlers in `AdminDashboard` |
| Multithreading (background thread + EDT) | `SwingWorker` in `runAction` and `refreshAll` |
| Anonymous class | `new SwingWorker<>() { … }`, `new DefaultTableModel(…) { … }` |
| Method overriding + `@Override` | `doInBackground()`, `done()`, `isCellEditable()` |
| Nested interface + lambda | `ServiceAction` in `AdminDashboard` |
| Transactions | `AdminDao.approveDoctor` |

## Step 6: Care-team linking — generics, functional interfaces and reuse

Patients add doctors and approve caregivers here. More importantly, this step removed repeated code by building `UiHelper`, a reusable class driven by **generics** and **functional interfaces**.

### The rules

- A patient picks a verified doctor; the link is `ACTIVE` immediately, because the patient's choice is the consent.
- A caregiver requests access by the patient's username; the link stays `PENDING` until the patient approves.
- Every SQL update includes the owner in its `WHERE` clause (`… AND patient_id = ?`). A patient can only approve requests on *their own* links, even if someone tampers with an ID.

### Problem: the same code in every window

`AdminDashboard` had its own `SwingWorker` code, its own read-only table model, its own "select a row first" check. Copying that into three more dashboards would mean four copies of every bug. The fix follows the **DRY** principle (Don't Repeat Yourself): write it once in `UiHelper`.

### `UiHelper`: generics + functional interfaces

```java
public final class UiHelper {
    private UiHelper() {}                                   // utility class: no objects

    @FunctionalInterface
    public interface ServiceCall<T> { T call() throws ServiceException; }

    @FunctionalInterface
    public interface ServiceAction { void run() throws ServiceException; }

    public static <T> void load(Component parent, ServiceCall<T> call, Consumer<T> onSuccess) {
        new SwingWorker<T, Void>() { … call.call() in the background, onSuccess.accept(result) on the EDT … }.execute();
    }

    public static void run(Component parent, ServiceAction action, Runnable onSuccess) {
        load(parent, () -> { action.run(); return null; }, ignored -> onSuccess.run());
    }
}
```

**Generics.** `<T>` is a *type parameter*: a placeholder for a real type, filled in by each caller. The same `load` method works for `List<CareLink>`, `List<DoctorOption>` or `DoseOverview`, and the compiler still checks every type. Without generics, we would return `Object` and cast everywhere, and mistakes would only show at runtime.

**Functional interfaces.** `ServiceCall` and `ServiceAction` each have exactly one abstract method, so a **lambda** or **method reference** can stand in for an object that implements them. `Consumer<T>` and `Runnable` are Java's own built-in functional interfaces.

Now a whole background task with error handling is one line in any window:

```java
UiHelper.load(this, careService::getMyDoctors, links -> { /* fill the table */ });
UiHelper.run(this, () -> careService.approveCaregiver(linkId), this::refreshCaregivers);
```

`careService::getMyDoctors` is a **method reference**: it says "call this method" without calling it yet. `UiHelper` decides *when* (in the background); the caller decides *what*. This is the **Strategy** idea: behaviour passed in as an object.

### A private generic helper in the service

```java
@FunctionalInterface
private interface SqlCall<T> { T run() throws SQLException; }

private <T> T db(SqlCall<T> call, String failMessage) throws ServiceException {
    try {
        return call.run();
    } catch (SQLException e) {
        throw new ServiceException(failMessage);
    }
}

return db(() -> linkDao.findDoctorsOfPatient(patientId), "Could not load your doctors.");
```

Every DAO call in `CareTeamService` goes through `db(…)`, so exception translation is written once.

### Overriding `toString()` in a record

```java
public record DoctorOption(int doctorId, String fullName, String specialization, String hospitalName) {
    @Override
    public String toString() { return "Dr. " + fullName + " - " + specialization + " (" + hospitalName + ")"; }
}
```

`JOptionPane.showInputDialog` shows a dropdown of any objects by calling their `toString()`. It knows nothing about doctors, yet each item prints nicely, because our override replaces the default. That is **polymorphism**: library code calls a method, and *our* version runs.

| OOP concept in this step | Where |
| --- | --- |
| Generics (generic method, type parameter) | `UiHelper.load`, `CareTeamService.db`, `List<CareLink>` |
| Functional interfaces | `ServiceCall<T>`, `ServiceAction`, `SqlCall<T>`, `Consumer`, `Runnable` |
| Lambdas and method references | `careService::getMyDoctors`, `() -> careService.approveCaregiver(id)` |
| Utility class (final, private constructor) | `UiHelper` |
| DRY / reuse | One `UiHelper` used by every dashboard |
| Overriding `toString()` (polymorphism) | `DoctorOption`, `PatientOption` |
| Security through ownership checks | `CareLinkDao` `WHERE … AND patient_id = ?` |

## Step 7: Prescriptions — abstract classes, inheritance and collections

Doctors now write prescriptions, and saving one generates every individual dose. This step introduced the clearest inheritance example in the project: the abstract class `BaseDao`.

### `BaseDao`: an abstract parent class

Every DAO needed the same helpers: fill a `PreparedStatement`, run an update, read one value, write an audit entry. Instead of copying them, we put them in one parent class:

```java
public abstract class BaseDao {
    protected PreparedStatement prepare(Connection c, String sql, Object... params) throws SQLException { … }

    protected int update(Connection c, String sql, Object... params) throws SQLException { … }  // inside a transaction
    protected int update(String sql, Object... params) throws SQLException { … }                // on its own

    protected <T> T queryValue(String sql, Class<T> type, Object... params) throws SQLException { … }
    protected void audit(Connection c, int actorUserId, String action, String entity, int entityId) throws SQLException { … }
}

public class PrescriptionDao extends BaseDao { … }
public class DoseDao         extends BaseDao { … }
public class MedicationDao   extends BaseDao { … }
```

- **`abstract`** means you can never write `new BaseDao()`. It exists only to be extended.
- **Inheritance ("is-a"):** `PrescriptionDao` *is a* `BaseDao`, so it gets `prepare`, `update`, `queryValue` and `audit` without writing them.
- **`protected`** means only subclasses (and the same package) can call these helpers. The UI cannot.
- **Overloading:** there are two `update` methods with the same name but different parameters. Java picks the right one from the arguments you pass.
- **Varargs** (`Object... params`) let one method accept any number of SQL parameters.

### `DoseGenerator`: one class, one job

```java
public final class DoseGenerator {
    private DoseGenerator() {}

    public static List<LocalDateTime> generate(PrescriptionItemRequest item, LocalDateTime notBefore) {
        List<LocalDateTime> doses = new ArrayList<>();
        for (LocalDate day = item.startDate(); !day.isAfter(item.endDate()); day = day.plusDays(item.intervalDays())) {
            for (LocalTime time : item.doseTimes()) {
                LocalDateTime at = LocalDateTime.of(day, time);
                if (!at.isBefore(notBefore)) doses.add(at);      // skip doses already in the past
            }
        }
        return doses;
    }
}
```

This is the heart of the system. It has no database code and no GUI code, only the rule "turn a schedule into dose times". That is the **Single Responsibility Principle**: a class should have one reason to change. It also makes the rule easy to test on its own.

### Collections used for a reason

| Collection | Where | Why this one |
| --- | --- | --- |
| `TreeSet<LocalTime>` | Parsing dose times ("20:00, 08:00, 08:00") | A *set* removes duplicates; a *tree* keeps them sorted, giving 08:00, 20:00 |
| `ArrayList<LocalDateTime>` | Generated doses | Ordered and grows as needed |
| `List<List<LocalDateTime>>` | Doses per prescription item | A list of lists, one inner list per medication |
| Streams | `items.stream().anyMatch(i -> i.medication().medicationId() == med.medicationId())` | Stops the same medication being added twice |

### A record with behaviour

Records can have their own methods, including static ones:

```java
public record PrescriptionItemRequest(MedicationOption medication, BigDecimal dosageAmount, String dosageUnit,
                                      List<LocalTime> doseTimes, int intervalDays,
                                      LocalDate startDate, LocalDate endDate, String instructions) {
    public String doseText() { return dosageAmount.stripTrailingZeros().toPlainString() + " " + dosageUnit; }

    public static String repeatText(int intervalDays) {
        return switch (intervalDays) { case 1 -> "Every day"; case 7 -> "Once a week"; default -> "Every " + intervalDays + " days"; };
    }
}
```

`BigDecimal` stores doses like 0.5 exactly, where a `double` could store 0.49999…

### Saving: one transaction, batch inserts

`PrescriptionDao.createPrescription` saves the prescription, its items, the dose times and every dose in **one transaction**. Doses are sent with `addBatch()` and `executeBatch()`, so 90 rows travel to MySQL in one trip.

### `PrescriptionDialog`: passing objects between windows

`PrescriptionDialog extends JDialog` and is **modal**: it blocks the dashboard until closed. Its constructor receives the selected patient as an object:

```java
PrescriptionDialog dialog = new PrescriptionDialog(this, patient);
dialog.setVisible(true);                // waits here until the dialog closes
if (dialog.isSaved()) refreshPrescriptions();
```

The dialog keeps `saved` as a `private boolean` and exposes it only through `isSaved()`: encapsulation again.

| OOP concept in this step | Where |
| --- | --- |
| Abstract class | `BaseDao` |
| Inheritance ("is-a") + `protected` | `PrescriptionDao`, `DoseDao`, `MedicationDao`, `ReminderDao`, `UserDao extends BaseDao` |
| Method overloading | `BaseDao.update(…)` ×2 |
| Varargs | `Object... params` |
| Single Responsibility Principle | `DoseGenerator` |
| Collections and streams | `TreeSet`, `ArrayList`, `List<List<…>>`, `stream().anyMatch()` |
| Record with instance + static methods | `PrescriptionItemRequest` |
| Modal dialog, objects passed between windows | `PrescriptionDialog` |

## Step 8: Dose tracking and adherence — composition, reuse and a background thread

Patients and caregivers now record doses and see adherence. One reusable panel serves three dashboards, and a background thread marks missed doses even when nobody is clicking.

### `DoseAdherencePanel`: build once, use three times

Instead of designing the dose screen separately for patients, caregivers and doctors, we built it once as a `JPanel` and placed it inside each dashboard:

```java
public class DoseAdherencePanel extends javax.swing.JPanel {
    public void showPatient(Integer patientId) { … }   // null = "the logged-in patient"
    public void showNothing(String message)    { … }
    public void refresh()                      { … }
    public void setReadOnly(boolean readOnly)  { … }   // doctors: hide Mark Taken / Skip
}

// in PatientDashboard
private final DoseAdherencePanel dosePanel = new DoseAdherencePanel();
pnlMedications.add(dosePanel, BorderLayout.CENTER);
dosePanel.showPatient(null);
```

- **Composition ("has-a").** A `PatientDashboard` *has a* `DoseAdherencePanel`; it does not *extend* it. Each dashboard is built from smaller parts, like bricks.
- **Encapsulation.** The dashboards never touch the panel's tables or buttons. They only call its four public methods. The panel can be redesigned without changing any dashboard.
- **Reuse.** One fix in the panel fixes all three dashboards.

### `DoseService.resolvePatient`: one gate for every role

```java
private int resolvePatient(Integer requestedPatientId) throws ServiceException {
    User u = Session.getUser();
    if (u.getRole() == Role.PATIENT)   return ownPatientId(u);                 // always their own records
    if (u.getRole() == Role.CAREGIVER) return checkCaregiverLink(u, requestedPatientId);
    if (u.getRole() == Role.DOCTOR)    return checkDoctorLink(u, requestedPatientId);
    throw new ServiceException("You are not allowed to view dose records here.");
}
```

Every read of dose data, every recording of a dose, and later every PDF export passes through this one method. Access rules written once cannot drift apart. A patient *always* gets their own records, whatever ID the screen sends.

### Derived data in a record

```java
public record AdherenceStats(String medication, int taken, int missed, int skipped) {
    public int total()      { return taken + missed + skipped; }
    public Double percent() { return total() == 0 ? null : 100.0 * taken / total(); }
}
```

The percentage is **calculated, not stored**, so it can never be out of date. Returning `null` when there are no doses avoids a false 0%.

### `MissedDoseJob`: a background thread on a timer

```java
public static synchronized void start() {
    if (executor != null) return;                         // start only once
    executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "missed-dose-job");
        t.setDaemon(true);                                // don't keep the app alive on exit
        return t;
    });
    executor.scheduleAtFixedRate(MissedDoseJob::runOnce, 0, 5, TimeUnit.MINUTES);
}
```

- **`ScheduledExecutorService`** runs `runOnce` immediately at startup, then every 5 minutes, on its own thread.
- **`synchronized`** stops two threads from starting the job at the same time.
- A **daemon thread** dies automatically when the last window closes.
- `runOnce` catches *every* exception. An uncaught exception would silently stop all future runs.

### Multithreading in DawaCare at a glance

| Thread | What runs on it | Tool |
| --- | --- | --- |
| Event Dispatch Thread | Drawing windows, clicks, updating tables | Swing itself |
| Short background workers | Each database call from a window | `SwingWorker` via `UiHelper` |
| `missed-dose-job` | Marking missed doses, completing prescriptions | `ScheduledExecutorService` |
| EDT timer (Step 9) | Checking for reminders each minute | `javax.swing.Timer` |

| OOP concept in this step | Where |
| --- | --- |
| Composition ("has-a") | Dashboards contain a `DoseAdherencePanel` |
| Encapsulation of a component | The panel's public methods only |
| Inheritance | `DoseAdherencePanel extends JPanel` |
| Centralised access control | `DoseService.resolvePatient` |
| Records with derived values | `AdherenceStats.percent()` |
| Multithreading: executors, daemon threads, `synchronized` | `MissedDoseJob` |

## Step 9: Doctor view, reminders and the medication catalogue

Three features were added on top of the existing classes, mostly by **reusing** them and **extending** them slightly. That is the payoff of good OOP design: new features cost less.

### Doctor adherence view: reuse with one switch

The doctor's *Adherence* tab is the same `DoseAdherencePanel` again, with one new method:

```java
public void setReadOnly(boolean readOnly) {
    btnSkip.setVisible(!readOnly);
    btnTaken.setVisible(!readOnly);
}
```

Hiding buttons is only cosmetic, so the service blocks doctors too: `DoseService.record` throws *"Doctors can view dose records but cannot record doses"* before touching the database. Checking the same rule in two layers is called **defence in depth**.

### Reminders: `ReminderPoller`

```java
public final class ReminderPoller {
    private final Timer timer;

    private ReminderPoller(JFrame owner) {                 // private constructor
        timer = new Timer(60_000, e -> check());           // every minute, on the EDT
        owner.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) { timer.stop(); }   // logout stops reminders
        });
        timer.start();
    }

    public static void attachTo(JFrame owner) { new ReminderPoller(owner); }   // static factory method
}
```

- **Static factory method.** Callers write `ReminderPoller.attachTo(this)` instead of `new ReminderPoller(this)`. The name says what it does, and the constructor stays private.
- **Adapter class.** `WindowListener` has seven methods. `WindowAdapter` implements all seven as empty methods, so our anonymous subclass overrides only the one we need, `windowClosed`.
- **`javax.swing.Timer`** fires on the EDT, and the actual database check still runs in a `SwingWorker`.
- **Each reminder appears once.** The `notifications` table has `UNIQUE (user_id, type, dose_id)`. `INSERT IGNORE` returns 1 for a new reminder and 0 for one already shown, so the database itself prevents duplicates, even across restarts.

### Medication catalogue: rules that protect history

The admin can add, edit and delete medications, through a new `MedicationDialog`. The `Medication` record uses `Integer medicationId`, not `int`, so `null` can mean "not saved yet":

```java
if (clean.medicationId() == null) {
    medicationDao.insert(clean, admin.getUserId());          // new medication
    return;
}
boolean identityChanged = !Objects.equals(current.name(), name)
        || !Objects.equals(current.dosageForm(), form)
        || !Objects.equals(current.strength(), strength);
if (current.usageCount() > 0 && identityChanged)
    throw new ServiceException("This medication is already used in prescriptions, so its name, "
            + "form and strength can't change. Add a new medication instead.");
```

If an admin could change "Amlodipine 5mg" to "10mg", every past prescription would silently change meaning. The service protects that history. `Objects.equals` compares two values safely even when one is `null`.

| OOP concept in this step | Where |
| --- | --- |
| Reuse + small extension | `DoseAdherencePanel.setReadOnly()` |
| Defence in depth | Doctor block in `DoseService.record` |
| Static factory method, private constructor | `ReminderPoller.attachTo()` |
| Adapter class + anonymous subclass | `new WindowAdapter() { windowClosed … }` |
| Timer-driven events | `javax.swing.Timer` in `ReminderPoller` |
| Wrapper types for "no value" (`Integer` vs `int`) | `Medication.medicationId` |
| Business rules in the service layer | `MedicationService.save`, `delete` |

## Step 10: Finishing features — passwords, audit log and PDF reports

The last features reused almost every earlier idea, which shows the design holding up: new classes slotted into the existing layers without rewriting old ones.

### Change password: shared rules and a refactor

The password rules were first written inside `RegistrationService`. Changing a password needs the *same* rules, so they moved into one class:

```java
public final class PasswordPolicy {
    private PasswordPolicy() {}
    public static void validate(String password, String confirm) throws ServiceException {
        if (password == null || password.length() < 8
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*"))
            throw new ServiceException("Password must be at least 8 characters with letters and numbers.");
        if (!password.equals(confirm))
            throw new ServiceException("Passwords do not match.");
    }
}
```

Two more details are worth explaining:

- `UserDao` was changed to `extends BaseDao` so it could use the inherited `update` and `audit` helpers. Inheritance made this a one-line change.
- `AuthService.changePassword` **re-reads** the user from the database instead of trusting the `User` object in `Session`. That object was loaded at login and could be out of date.

### Audit log viewer: interfaces with several methods

The admin's *Audit Log* tab filters rows as the admin types:

```java
txtAuditSearch.getDocument().addDocumentListener(new DocumentListener() {
    @Override public void insertUpdate(DocumentEvent e)  { applyAuditFilter(); }
    @Override public void removeUpdate(DocumentEvent e)  { applyAuditFilter(); }
    @Override public void changedUpdate(DocumentEvent e) { applyAuditFilter(); }
});
```

`DocumentListener` has **three** abstract methods, so it is *not* a functional interface and a lambda cannot replace it. An anonymous class must implement all three. Compare this with `ActionListener` (one method), where a lambda works. A `TableRowSorter` with a `RowFilter` then hides the non-matching rows without reloading anything.

The `AuditEntry` record turns raw values into readable text with its own methods: `actionText()` turns `APPROVE_DOCTOR` into "Approve doctor".

### Prescription auto-complete

`MissedDoseJob` gained a second task: once an item's end date has passed and none of its doses are `PENDING`, mark it `COMPLETED`; when all items are done, complete the prescription. Order matters: missed doses are marked first, then prescriptions are completed.

### PDF reports: separating gathering from drawing

The report is split across three classes, each with one job:

| Class | Job | Knows about |
| --- | --- | --- |
| `DoseService.getReportData()` | Checks access and gathers the data | The database (through `DoseDao`), `Session` |
| `AdherenceReportData` (record) | Carries the data | Nothing else |
| `AdherenceReportPdf.write()` | Draws the PDF | OpenPDF only, never the database |

Because `getReportData()` calls the same `resolvePatient` gate as the screen, nobody can export a report for a patient they cannot see. Each export also writes a `GENERATE_REPORT` audit entry.

Inside `AdherenceReportPdf`, two overloaded helpers keep the table code short:

```java
private static void cell(PdfPTable t, Object value)            { cell(t, value, TEXT); }
private static void cell(PdfPTable t, Object value, Font font) { … }
```

The first overload supplies a default font and calls the second. Parameter type `Object` lets any value (a number, a date, a string) be printed through its `toString()`, which is polymorphism again.

Writing the file uses **file I/O** with try-with-resources (`new FileOutputStream(file)`). The UI wraps an `IOException` into a `ServiceException`, so the usual `UiHelper` error dialog handles it.

| OOP concept in this step | Where |
| --- | --- |
| DRY, shared static rules | `PasswordPolicy` |
| Inheritance used to refactor | `UserDao extends BaseDao` |
| Anonymous class implementing a multi-method interface | `DocumentListener` in `AdminDashboard` |
| Functional vs non-functional interfaces | `ActionListener` (lambda) vs `DocumentListener` (anonymous class) |
| Separation of concerns | `getReportData` / `AdherenceReportData` / `AdherenceReportPdf` |
| Overloading with a default | `AdherenceReportPdf.cell(…)` |
| File I/O + exception wrapping | `AdherenceReportPdf.write`, `exportPdf` |

## Step 11: Packaging and publishing

The finished system runs without NetBeans as one file, `DawaCare.jar`, and the source is published on GitHub without any passwords.

### What we did

1. Added the **Maven Shade plugin** to `pom.xml`. It packs our classes plus the MySQL driver, jBCrypt and OpenPDF into one runnable jar, and records `com.mycompany.dawacare.DawaCare` as the class to start.
2. Let `DBConnection` read a `db.properties` file **next to the jar** first, and fall back to the built-in one. Another computer can use its own MySQL password without rebuilding.
3. Added a `.gitignore` so `db.properties` (the real password) and `target/` (build output) are never uploaded. A `db.properties.example` with `CHANGE_ME` shows others what to fill in.
4. Added `database/schema.sql` and `database/seed_medications.sql` so anyone can recreate the database.
5. Committed in small steps (one commit per working feature) and pushed to GitHub, then added a README with screenshots and a sample report.

### The OOP lesson: encapsulation pays off

We changed *how* `DBConnection` finds its settings, but its public method stayed the same: `DBConnection.getConnection()`. Not one of the classes that call it had to change. That is the practical value of encapsulation: **hide the details behind a stable method, and the details can change freely.**

| OOP concept in this step | Where |
| --- | --- |
| Encapsulation (stable interface, hidden change) | `DBConnection.getConnection()` |
| Configuration outside the code | External `db.properties` |
| Entry point (`public static void main`) | `DawaCare` class, named in the jar's manifest |

## Deep dive: the four pillars of OOP, taught with DawaCare

Every OOP course rests on four ideas: encapsulation, abstraction, inheritance and polymorphism. Below, each one gets a plain definition, why it matters, and the clearest DawaCare examples to quote in your presentation.

### 1. Encapsulation — protect the data

**Definition:** keep an object's data private, and let other code change it only through methods that enforce the rules.

**Why it matters:** if any class could change any field, one bug anywhere could corrupt data everywhere. Encapsulation puts each rule in exactly one place.

| Example | What is hidden | What is exposed |
| --- | --- | --- |
| `User` | All fields (`private`) | Getters only, no setters, so it cannot be changed after loading |
| `Session` | `currentUser` (`private static`) | `start`, `end`, `getUser`, `hasRole` |
| `DBConnection` | The settings and password | `getConnection()` only |
| `DoseAdherencePanel` | Its tables, buttons and loading logic | `showPatient`, `showNothing`, `refresh`, `setReadOnly` |
| `PrescriptionDialog` | The `saved` flag | `isSaved()` |

**Say it like this:** "The dashboards never touch the dose panel's tables directly. They call four public methods, so I could redesign the panel without changing a single dashboard."

### 2. Abstraction — show what, hide how

**Definition:** give users of a class a simple view of *what* it does, and hide *how* it does it.

**Why it matters:** the UI stays simple, and the hidden parts can change freely.

- **The layers are abstraction.** `LoginFrame` calls `authService.login(u, p)`. It never sees SQL, BCrypt or status rules.
- **Abstract class:** `BaseDao` cannot be created on its own; it describes what every DAO can do.
- **Interfaces:** `ServiceCall<T>` only says "something that returns a `T` and may throw `ServiceException`". `UiHelper` runs it without knowing what it does.

**Abstract class vs interface, as used in DawaCare:**

|  | Abstract class (`BaseDao`) | Interface (`ServiceCall<T>`) |
| --- | --- | --- |
| Can hold working code | Yes: `prepare`, `update`, `audit` | Only a method signature (plus optional default methods) |
| Can hold fields | Yes | Only constants |
| A class can extend / implement | Only one parent class | Many interfaces |
| Used for | Sharing code between related classes | A contract that any code (even a lambda) can fulfil |

### 3. Inheritance — reuse through "is-a"

**Definition:** a subclass gets the fields and methods of its parent and can add or change behaviour.

**Why it matters:** shared code is written once in the parent.

&#91;embedded content: class hierarchy · 5 parent classes, 17 subclasses\]

Each child inherits its parent's methods for free: every DAO gets `prepare`, `update`, `queryValue` and `audit` from `BaseDao`, and every window gets `setVisible`, `dispose` and `setTitle` from `JFrame`. Anonymous subclasses (`SwingWorker`, `DefaultTableModel`, `WindowAdapter`) are inheritance too, just without a class name.

**When we chose *not* to use inheritance:** roles. A doctor is not a different kind of object for logging in; a `User` *has* a `Role` and a profile row. And the dashboards *contain* a `DoseAdherencePanel` rather than extending it. The rule of thumb: use inheritance for a true "is-a", composition for "has-a".

### 4. Polymorphism — one call, many behaviours

**Definition:** the same method call behaves differently depending on the object it is called on.

| Kind | When it is decided | DawaCare example |
| --- | --- | --- |
| **Overriding** (runtime) | While the program runs, by the real object type | `toString()` in `DoctorOption`; `isCellEditable()`; `doInBackground()` and `done()` |
| **Dynamic dispatch** (runtime) | Same | `JFrame dashboard = new DoctorDashboard(); dashboard.setVisible(true);` |
| **Overloading** (compile time) | When compiling, by the argument types | `BaseDao.update(String, …)` vs `update(Connection, String, …)`; `cell(t, value)` vs `cell(t, value, font)` |
| **Interface polymorphism** | Runtime | Any lambda passed as `ServiceCall<T>` to `UiHelper.load` |

**Say it like this:** "`JOptionPane` was written years before DawaCare and knows nothing about doctors. When it calls `toString()` on each option, *my* override runs and prints *Dr. Namara - surgeon (Nsambya)*."

### Other principles visible in the code

| Principle or pattern | Meaning | DawaCare example |
| --- | --- | --- |
| Single Responsibility | One class, one reason to change | `DoseGenerator`, `PasswordPolicy`, `AdherenceReportPdf` |
| DRY | Don't repeat yourself | `UiHelper`, `BaseDao`, `PasswordPolicy` |
| DAO pattern | All SQL for an entity in one class | `UserDao`, `PrescriptionDao`, `DoseDao` |
| Layered architecture | UI → service → DAO → database | Every feature |
| Factory | Code chooses which class to create | Dashboard `switch` in `LoginFrame`; `ReminderPoller.attachTo` |
| Singleton-like | One shared instance or state | `Session` |
| Observer | Objects notified of events | Swing listeners, `DocumentListener` |
| Strategy | Behaviour passed in as an object | Lambdas passed to `UiHelper.load` and `run` |

## Bugs we hit and the OOP lessons they teach

The errors during the build were not wasted time: each one demonstrates a rule of Java's object model. Examiners like hearing how you debugged, so these make good presentation stories.

| Symptom | Real cause | Fix | OOP lesson |
| --- | --- | --- | --- |
| `IllegalArgumentException: adding a window to a container` | `DoseAdherencePanel` had been created as a JFrame Form, so it `extends JFrame` | Recreated it as a JPanel Form (`extends JPanel`) | **Inheritance decides what an object *is*.** A `JFrame` *is a* window, and a window cannot sit inside another component. A `JPanel` *is a* component, so it can. |
| `ServiceException cannot be converted to Throwable` | The class was empty and did not `extend Exception` | Added `extends Exception` and a constructor calling `super(message)` | Only subclasses of `Throwable` can be thrown or caught. The type hierarchy is checked by the compiler. |
| `cannot find symbol: method register(…)` | `RegistrationService` was an empty skeleton | Pasted in the method | A method can only be called if the class actually declares (or inherits) it. |
| `cannot find symbol: variable lblStatus` / `tblUsers` / `btnApprove` | Components named differently in the GUI Builder (`jTable2`, `jButton3`) | Renamed them in the Navigator | Every GUI component is a **field** of the form class; code and design must agree on its name. |
| Card layout never switched | The three card panels were nested inside each other | Moved them so all three sit directly under `pnlRoleDetails` | A GUI is a **tree of objects** (containment). Where an object sits in the tree decides how it behaves. |
| Login stuck on "Signing in…" | The dashboard constructor threw an exception inside `SwingWorker.done()` | Fixed the layout, and wrapped `openDashboard` in `try/catch` | An uncaught exception on the EDT does not crash the app; it silently stops that action. Always handle exceptions where they can happen. |
| "is not attached to a horizontal group" style failures | Adding a component in code to a panel using GroupLayout | Set the host panel to `BorderLayout` | A container delegates sizing to a `LayoutManager` **object**. Swapping that object changes the behaviour: the **Strategy** pattern. |
| Project failed to compile after changing a constructor | NetBeans' generated `main` still called the old `PlaceholderDashboard()` | Deleted the generated `main` | Changing a constructor's parameters breaks every caller. Constructors are part of a class's public contract. |
| Dozens of errors in `DoctorDashboard` | One extra `}` closed the class early | Removed it | Methods and fields must be inside a class's braces; outside them, Java sees code that belongs to nothing. |
| `remote: Invalid username or token` | A saved, outdated GitHub credential | Regenerated the token | Not OOP, but the same idea as encapsulation: secrets live in one controlled place (`.gitignore`, a token), never in code. |

## Presentation plan, demo script and examiner questions

A 10–15 minute presentation works best as: the problem, the design, a live demo of the full loop, then the OOP highlights.

### Suggested running order

1. **The problem (1 min).** Patients forget or skip medication; doctors cannot see it. DawaCare tracks every dose and measures adherence.
2. **The design (2 min).** Show the architecture diagram: UI → service → DAO → MySQL. Mention the database redesign (doctor approval, patient consent, no cascade deletes).
3. **Live demo (5–6 min).** Follow the demo script below.
4. **OOP highlights (3–4 min).** Pick one strong example per pillar: `User` (encapsulation), `AuthService` / `BaseDao` (abstraction), `BaseDao` subclasses and `ServiceException` (inheritance), the dashboard `switch` and `toString()` (polymorphism). Then show `DoseAdherencePanel` reused in three dashboards.
5. **Lessons and limitations (1 min).** The "adding a window to a container" bug; reminders only work while the app is open; appointments are future work.

### Demo script

Prepare the accounts before you present, so the demo only shows the interesting parts.

1. **Register a doctor.** Show the doctor cannot log in yet ("awaiting admin approval").
2. **Log in as admin.** Approve the doctor in *Pending Doctors*. Briefly show the *Audit Log* entry.
3. **Log in as the patient.** *My Doctors → Add Doctor*, choose the new doctor. In *My Caregivers*, approve the caregiver's request.
4. **Log in as the doctor.** Select the patient → *New Prescription*: two medications, one with times a few minutes from now. Save and read out the "N scheduled doses" message.
5. **Log in as the patient.** Show today's doses, click *Mark Taken*, watch adherence turn green. If a reminder pops up, point it out.
6. **Log in as the caregiver.** Choose the patient from the dropdown; show the same dose recorded and "Recorded by".
7. **Export the PDF** from the doctor's *Adherence* tab and open it.

Back-up plan: keep the screenshots and `docs/sample-report.pdf` ready in case something fails live.

### Likely examiner questions

**Where is encapsulation in your project?** `User` has private fields and only getters, so a loaded user cannot be changed by accident. `DoseAdherencePanel` hides its tables behind four public methods, and `DBConnection` hides the database password behind `getConnection()`.

**Show me inheritance.** `BaseDao` is an abstract class with shared helpers; `PrescriptionDao`, `DoseDao`, `MedicationDao`, `ReminderDao` and `UserDao` extend it. `ServiceException extends Exception`, and every form extends `JFrame`, `JDialog` or `JPanel`.

**Where is polymorphism?** The login code stores any dashboard in a `JFrame` variable and calls `setVisible(true)`; the real subclass decides what opens. Overridden `toString()` in `DoctorOption` makes a library dialog print our doctors correctly. `BaseDao.update` is overloaded.

**Why didn't you make `Patient` and `Doctor` subclasses of `User`?** Login works the same for every role, and the role-specific data already lives in separate tables. So a `User` *has* a `Role` and a profile (composition) instead of being a subclass. I used inheritance where there is a true "is-a", such as the DAOs.

**Why BCrypt and not just storing passwords?** If the database leaks, plain passwords are exposed. BCrypt stores a salted, deliberately slow hash, so the original password cannot be read back and guessing is expensive.

**Why `PreparedStatement`?** It sends the values separately from the SQL, which prevents SQL injection, and it handles quoting and types for us.

**What is a transaction and where did you use one?** Several database changes that succeed or fail together. Registration (user + profile), saving a prescription (prescription + items + times + all doses), approving a doctor (status + verification + audit).

**Why `SwingWorker`?** Swing has one thread for drawing and clicks (the EDT). Database calls run on a background thread so the window never freezes; results are shown back on the EDT in `done()`.

**How is adherence calculated?** Taken ÷ (taken + missed + skipped), counting only doses whose time has passed. It is calculated when needed, never stored, so it cannot go out of date.

**How do you stop a caregiver seeing a patient who didn't approve them?** Every dose request goes through `DoseService.resolvePatient`, which checks for an `ACTIVE` link in `caregiver_patients`. The same check protects the PDF export.

**What would you improve?** SMS or email reminders (the desktop app can only remind while open), appointment and medical-condition screens, automated unit tests for `DoseGenerator` and the services, and charts of adherence over time.

## Glossary

| Term | Meaning |
| --- | --- |
| Abstract class | A class marked `abstract` that cannot be instantiated, only extended (`BaseDao`) |
| Anonymous class | A subclass or interface implementation with no name, written where it is used (`new SwingWorker<>() { … }`) |
| Checked exception | An exception the compiler forces you to catch or declare (`ServiceException`, `SQLException`) |
| Composition | Building a class from other objects it holds ("has-a") |
| Constructor | A special method that sets up a new object; same name as the class |
| DAO | Data Access Object: a class that holds all the SQL for one kind of data |
| Dynamic dispatch | Choosing which overridden method runs from the object's real type, at runtime |
| EDT | Event Dispatch Thread: Swing's single thread for drawing and handling clicks |
| Encapsulation | Keeping data private and exposing only safe methods |
| Enum | A type with a fixed set of named constants (`Role`) |
| Functional interface | An interface with exactly one abstract method; a lambda can implement it |
| Generics | Type parameters such as `<T>` that let code work with any type, safely |
| Immutable | Cannot be changed after creation (records, `User` with no setters) |
| Inheritance | A subclass reusing and extending a parent class (`extends`) |
| Interface | A contract of method signatures a class promises to provide (`implements`) |
| Lambda | A short anonymous function: `() -> careService.approveCaregiver(id)` |
| Method reference | A shortcut for a lambda that calls one method: `careService::getMyDoctors` |
| Modal dialog | A window that blocks its parent until closed (`PrescriptionDialog`) |
| Overloading | Same method name, different parameter lists; chosen at compile time |
| Overriding | A subclass replacing a parent's method with the same signature; chosen at runtime |
| Polymorphism | One call, different behaviour depending on the object |
| `PreparedStatement` | A pre-compiled SQL statement with `?` placeholders; prevents SQL injection |
| Record | A compact immutable data class (`record DoseItem(…)`) |
| `static` | Belongs to the class rather than to an object |
| `super` | Refers to the parent class: its constructor or methods |
| Transaction | Several database changes that succeed or fail together (`commit` / `rollback`) |
| try-with-resources | `try (…)` that closes connections, statements and files automatically |
| Utility class | A `final` class with a private constructor and only static methods (`UiHelper`) |
