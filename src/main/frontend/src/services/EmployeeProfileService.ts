import {
    EmployeeProfileControllerApi,
    EmployeeProfileCreateDTO,
    EmployeeProfileDTO,
    EmployeeProfileUpdateDTO,
} from "../generated-skeleton-api";
import { apiConfig } from "./apiConfig";

const api = new EmployeeProfileControllerApi(apiConfig);

export const EmployeeProfileService = {
    getMe: (): Promise<EmployeeProfileDTO> =>
        api.getMe().then(r => r.data),

    getAll: (): Promise<EmployeeProfileDTO[]> =>
        api.getAll4().then(r => r.data),

    getById: (id: number): Promise<EmployeeProfileDTO> =>
        api.getById4({ id }).then(r => r.data),

    create: (employeeProfileCreateDTO: EmployeeProfileCreateDTO): Promise<EmployeeProfileDTO> =>
        api.create4({ employeeProfileCreateDTO }).then(r => r.data),

    update: (id: number, employeeProfileUpdateDTO: EmployeeProfileUpdateDTO): Promise<EmployeeProfileDTO> =>
        api.update4({ id, employeeProfileUpdateDTO }).then(r => r.data),

    delete: (id: number): Promise<void> =>
        api.delete4({ id }).then(() => undefined),
};
