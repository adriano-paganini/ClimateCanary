/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import { UserxDTO, UserxRole } from "../generated-skeleton-api";

export type UserxValidationResult = {
  valid: boolean;
  message?: string;
  fieldErrors?: Partial<
    Record<keyof UserxDTO | "password" | "departmentId" | "roomId", string>
  >;
};

export const rolesToArray = (
  roles?: Set<UserxRole> | UserxRole[] | string[],
): UserxRole[] => {
  if (!roles) return [];
  return Array.isArray(roles)
    ? createUserxRoleArrayFromStrings(roles)
    : [...roles];
};

export const hasUserRole = (
  user:
    | Pick<UserxDTO, "roles">
    | { roles?: Set<UserxRole> | UserxRole[] | string[] }
    | null,
  role: UserxRole,
): boolean => rolesToArray(user?.roles).includes(role);

const FORBIDDEN_ROLE_COMBINATIONS: Array<{
  pair: [UserxRole, UserxRole];
  reason: string;
}> = [
  {
    pair: [UserxRole.BUILDING_ADMIN, UserxRole.SYSTEM_ADMIN],
    reason: "Building Admin and System Admin are standalone roles. Select only one of them.",
  },
  {
    pair: [UserxRole.SYSTEM_ADMIN, UserxRole.EMPLOYEE],
    reason: "System Admin is a standalone role and cannot be combined with Employee.",
  },
  {
    pair: [UserxRole.BUILDING_ADMIN, UserxRole.EMPLOYEE],
    reason: "Building Admin is a standalone role and cannot be combined with Employee.",
  },
];

export function getRoleCombinationError(roles: UserxRole[]): string | null {
  for (const {
    pair: [a, b],
    reason,
  } of FORBIDDEN_ROLE_COMBINATIONS) {
    if (roles.includes(a) && roles.includes(b)) {
      return reason;
    }
  }
  if (
    roles.includes(UserxRole.MANAGEMENT) &&
    roles.some((r) => r !== UserxRole.MANAGEMENT)
  ) {
    return "Management is a standalone role. Select only Management or choose a different role set.";
  }
  return null;
}

export function getImpliedRoles(roles: UserxRole[]): UserxRole[] {
  const implied: UserxRole[] = [];
  if (roles.includes(UserxRole.DEPARTMENT_LEAD)) {
    implied.push(UserxRole.EMPLOYEE);
  }
  return implied;
}

export const createUserxRoleArrayFromStrings = (
  roles: string[],
): UserxRole[] => {
  return roles.map((role) => {
    switch (role) {
      case UserxRole.SYSTEM_ADMIN:
        return UserxRole.SYSTEM_ADMIN;
      case UserxRole.BUILDING_ADMIN:
        return UserxRole.BUILDING_ADMIN;
      case UserxRole.DEPARTMENT_LEAD:
        return UserxRole.DEPARTMENT_LEAD;
      case UserxRole.MANAGEMENT:
        return UserxRole.MANAGEMENT;
      case UserxRole.EMPLOYEE:
        return UserxRole.EMPLOYEE;
      default:
        throw new Error(`Invalid role: ${role}`);
    }
  });
};
