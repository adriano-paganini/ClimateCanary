import {
    AbsenceControllerApi,
    AbsenceCreateDTO,
    AbsenceDTO,
    AbsenceUpdateDTO,
} from "../generated-skeleton-api";
import { apiConfig } from "./apiConfig";

const api = new AbsenceControllerApi(apiConfig);

export const AbsenceService = {
    getAll: (params: {
        userxId?: number;
        departmentId?: number;
    } = {}): Promise<AbsenceDTO[]> =>
        api.getAll8(params).then(r => r.data),

    getById: (id: number): Promise<AbsenceDTO> =>
        api.getById10({ id }).then(r => r.data),

    create: (absenceCreateDTO: AbsenceCreateDTO): Promise<AbsenceDTO> =>
        api.create10({ absenceCreateDTO }).then(r => r.data),

    update: (id: number, absenceUpdateDTO: AbsenceUpdateDTO): Promise<AbsenceDTO> =>
        api.update10({ id, absenceUpdateDTO }).then(r => r.data),

    delete: (id: number): Promise<void> =>
        api.delete10({ id }).then(() => undefined),
};
