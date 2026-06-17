// Mirrors the backend DTOs (see *Response records + enums).

export type MemberStatus = "ACTIVE" | "INACTIVE" | "SUSPENDED" | "TERMINATED";
export type RecipientGroup = "ACTIVE_MEMBERS" | "ALL_MEMBERS";
export type Role = "ADMIN" | "MEMBER";

export interface LoginResponse {
  token: string;
  mustChangePassword: boolean;
}

export interface Member {
  id: string;
  membershipNumber: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string; // ISO date
  email: string;
  phone: string | null;
  address: string | null;
  joinDate: string; // ISO date
  status: MemberStatus;
}

export interface DuesRecord {
  id: string;
  memberId: string;
  year: number;
  paid: boolean;
  paidDate: string | null;
}

export interface AnnouncementResponse {
  recipientCount: number;
}

// Spring Data Page<T>
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // current page (0-based)
  size: number;
  first: boolean;
  last: boolean;
}

export interface RegisterMemberInput {
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  email: string;
  phone?: string;
  address?: string;
}
