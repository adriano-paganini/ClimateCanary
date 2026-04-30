import {
    DepartmentControllerApi,
    DepartmentCreateDTO,
    DepartmentDTO,
    DepartmentUpdateDTO,
    RoomDTO,
    UserxDTO,
} from "../generated-skeleton-api";
import { apiConfig } from "./apiConfig";

const api = new DepartmentControllerApi(apiConfig);

export const DepartmentService = {
    getAll: (): Promise<DepartmentDTO[]> =>
        api.getAll4().then(r => r.data),

    getById: (id: number): Promise<DepartmentDTO> =>
        api.getById5({ id }).then(r => r.data),

    getDepartmentLeader: (id: number): Promise<UserxDTO> =>
        api.getDepartmentLeader({ id }).then(r => r.data),

    getRooms: (id: number): Promise<RoomDTO[]> =>
        api.getRooms({ id }).then(r => r.data),

    create: (departmentCreateDTO: DepartmentCreateDTO): Promise<DepartmentDTO> =>
        api.create5({ departmentCreateDTO }).then(r => r.data),

    update: (id: number, departmentUpdateDTO: DepartmentUpdateDTO): Promise<DepartmentDTO> =>
        api.update5({ id, departmentUpdateDTO }).then(r => r.data),

    delete: (id: number): Promise<void> =>
        api.delete5({ id }).then(() => undefined),
};
