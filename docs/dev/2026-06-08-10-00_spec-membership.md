# Taraku Enlightenment Club — Membership App Specification

**Date:** 2026-06-08
**Status:** Draft

---

## Overview

A web-based membership management application for the Taraku Enlightenment Club (TEC). Admins register and manage members; members can log in to view and update their own profiles and track dues history. The system sends automated email notifications for key lifecycle events.

---

## Roles

| Role | Description |
|------|-------------|
| **Admin** | Club secretaries/officers. Can manage all members, record dues, send announcements. Multiple admins supported. |
| **Member** | Registered club members. Can view and update their own profile and view dues history. |

---

## Domain Model

### Member

| Field | Type | Notes |
|-------|------|-------|
| `membershipNumber` | String | Auto-generated, format: `TEC-YYYY-NNN` (e.g., `TEC-2024-001`) |
| `firstName` | String | |
| `lastName` | String | |
| `dateOfBirth` | Date | |
| `email` | String | Unique; used as login credential |
| `phone` | String | |
| `address` | String | |
| `joinDate` | Date | Set at registration |
| `status` | Enum | `ACTIVE`, `INACTIVE`, `SUSPENDED`, `TERMINATED` |

### DuesRecord

| Field | Type | Notes |
|-------|------|-------|
| `year` | Integer | Dues year (e.g., 2024) |
| `paid` | Boolean | Set manually by Admin |
| `paidDate` | Date | Date Admin marked as paid (nullable) |
| `member` | Member | Owning member |

### Annual Dues Rules
- Deadline: **end of April** each year (April 30)
- Members who have not been marked as paid by May 1 are **automatically set to INACTIVE**
- Dues reminder email sent **30 days before deadline** (April 1)
- Overdue notice sent **on May 1** to unpaid members

---

## Spring Modulith Module Structure

```
com.taraku.membership
├── member/            # Member registration, profile, status management
│   ├── domain/
│   │   ├── models/    # Member, MemberStatus (NamedInterface)
│   │   └── ...
│   ├── MemberAPI.java # Public API facade
│   └── ...
├── dues/              # Annual dues tracking, auto-inactivation
│   ├── domain/
│   │   ├── models/    # DuesRecord (NamedInterface)
│   │   └── ...
│   ├── DuesAPI.java
│   └── ...
├── notification/      # Email notifications (event-driven)
│   ├── NotificationAPI.java
│   └── ...
└── shared/            # Common value objects, exceptions (OPEN module)
```

**Inter-module communication:** via Spring Modulith application events (no direct service-to-service calls across modules).

---

## User Journeys

### Journey 1: Admin Registers a New Member

1. Admin logs in → navigates to **Members → Register New Member**
2. Fills in: first name, last name, DOB, email, phone, address
3. System auto-generates `membershipNumber` (TEC-YYYY-NNN, sequential within year)
4. System sets `status = ACTIVE`, `joinDate = today`
5. System creates a dues record for the current year (unpaid)
6. System sends **welcome email** to member with temporary password and login instructions
7. Admin lands on the member's profile page; success toast shown

**Edge cases:**
- Email already registered → validation error shown, no duplicate created
- Admin can cancel mid-form without side effects

---

### Journey 2: Member First Login & Profile Setup

1. Member receives welcome email → clicks login link
2. Logs in with email + temporary password
3. System prompts password change on first login
4. Member lands on **My Profile** page
5. Member can edit: phone, address (not email, DOB, membershipNumber, joinDate, status — these are read-only)
6. Member saves → success confirmation shown

---

### Journey 3: Member Views Dues History

1. Member logs in → navigates to **My Dues**
2. Sees list of dues records by year: year, status (Paid / Unpaid), paid date if paid
3. Read-only — member cannot mark their own dues

---

### Journey 4: Admin Records Dues Payment

1. Admin navigates to **Members → [Select Member] → Dues**
2. Sees dues history for that member
3. Clicks **Mark Paid** for the current year
4. System sets `paid = true`, `paidDate = today`
5. If member was INACTIVE due to non-payment, system sets `status = ACTIVE`
6. System sends **dues confirmed** email to member

**Edge cases:**
- Already paid → button disabled / idempotent
- Admin can mark past years paid (back-fill)

---

### Journey 5: Automatic Dues Inactivation (May 1 Scheduled Job)

1. On May 1 each year, system runs a scheduled job
2. Finds all ACTIVE members with unpaid dues for the current year
3. Sets their `status = INACTIVE`
4. Sends **overdue/inactivation notice** email to each affected member
5. Admin can see status change in member list

---

### Journey 6: Admin Changes Member Status Manually

1. Admin navigates to member profile
2. Clicks **Change Status** → selects new status (ACTIVE / SUSPENDED / TERMINATED)
3. Optionally enters a reason note
4. System updates status
5. System sends **status change notification** email to member

**Edge cases:**
- Cannot activate a TERMINATED member (terminal state — requires re-registration)
- TERMINATED members cannot log in

---

### Journey 7: Admin Sends Announcement

1. Admin navigates to **Communications → New Announcement**
2. Enters subject and message body
3. Selects recipient group: All Active Members / All Members
4. Clicks **Send**
5. System sends email to selected group
6. Confirmation shows count of emails sent

---

### Journey 8: Admin Views Member List

1. Admin navigates to **Members**
2. Sees paginated list with: name, membership number, status, dues status for current year
3. Can filter by: status, dues paid/unpaid
4. Can search by name or membership number
5. Clicks a member → goes to member profile

---

### Journey 9: Password Reset

1. Member (or Admin) clicks **Forgot Password** on login page
2. Enters email → system sends reset link (valid 24h)
3. Clicks link → enters new password
4. Redirected to login

---

## Email Notifications

| Trigger | Recipient | Content |
|---------|-----------|---------|
| Member registered | Member | Welcome + temporary password + login link |
| Dues marked paid | Member | Confirmation of payment for year |
| April 1 (scheduled) | All unpaid active members | Dues reminder — due by April 30 |
| May 1 (scheduled) | All newly inactivated members | Overdue notice + status change to INACTIVE |
| Status changed manually | Member | New status + reason (if provided) |
| Admin announcement | Selected group | Custom subject + body |
| Password reset requested | Member/Admin | Reset link (24h expiry) |

---

## User Stories

- **US-1**: As an Admin, I navigate to the Members section so that I can manage club membership.
- **US-2**: As an Admin, I register a new member with their personal details so that they are officially enrolled in the club with an auto-generated membership number.
- **US-3**: As an Admin, I view a paginated, searchable, and filterable member list so that I can quickly find and manage any member.
- **US-4**: As an Admin, I view a member's full profile so that I can see their personal details, status, and dues history.
- **US-5**: As an Admin, I manually change a member's status (Active/Suspended/Terminated) with an optional reason so that the roster reflects the member's current standing.
- **US-6**: As an Admin, I mark a member's annual dues as paid so that their payment is recorded and their status is updated if previously inactive.
- **US-7**: As an Admin, I send an announcement email to all active members (or all members) so that I can communicate club news and events.
- **US-8**: As an Admin, I view a member's dues history across all years so that I can verify payment compliance over time.
- **US-9**: As a Member, I receive a welcome email with a temporary password after registration so that I can log in for the first time.
- **US-10**: As a Member, I change my password on first login so that my account is secured.
- **US-11**: As a Member, I view my own profile including my membership number, status, and join date so that I know my current club standing.
- **US-12**: As a Member, I update my contact details (phone, address) so that the club has my current information.
- **US-13**: As a Member, I view my dues history by year so that I can confirm my payment records.
- **US-14**: As a Member, I reset my password via email so that I can regain access if I forget my credentials.
- **US-15**: As a Member, I receive an email reminder on April 1st so that I am notified to pay my dues before the April 30 deadline.
- **US-16**: As a Member, I receive an email on May 1st if my dues are unpaid so that I understand my status has been changed to INACTIVE.
- **US-17**: As a Member, I receive an email when my status changes so that I am informed of any changes to my membership standing.
- **US-18**: As the System, I automatically set unpaid ACTIVE members to INACTIVE on May 1st each year so that the roster reflects dues compliance without requiring manual admin intervention.
- **US-19**: As the System, I auto-generate a unique membership number in the format `TEC-YYYY-NNN` upon member registration so that each member has a traceable club identifier.

---

## Edge Cases & Constraints

- Duplicate email registration → rejected with validation error
- TERMINATED members cannot log in; they must be re-registered by an Admin
- Admin cannot self-terminate their own account
- Dues records are created automatically for each member at registration and at the start of each new dues year
- Re-activating an inactive member (dues paid after May 1) sets status back to ACTIVE
- Password reset links expire after 24 hours
- Multiple admins: all admins have equal privileges

---

## Tech Stack (Assumed)

- **Backend:** Spring Boot 3.x + Spring Modulith 2.x
- **Database:** PostgreSQL
- **Auth:** Spring Security (session-based or JWT)
- **Email:** Spring Mail (SMTP)
- **Scheduling:** Spring `@Scheduled` (May 1 inactivation job, April 1 reminder job)
- **Frontend:** Web (Thymeleaf or REST API + SPA — to be decided)
