import {
    ThresholdViolationControllerApi,
    ThresholdViolationCreateDTO,
    ThresholdViolationDTO,
    ThresholdViolationUpdateDTO,
} from "../generated-skeleton-api";
import { apiConfig } from "./apiConfig";

const api = new ThresholdViolationControllerApi(apiConfig);

export const ViolationService = {
    getAll: (): Promise<ThresholdViolationDTO[]> =>
        api.getAll().then(r => r.data),

    getById: (id: number): Promise<ThresholdViolationDTO> =>
        api.getById({ id }).then(r => r.data),

    create: (thresholdViolationCreateDTO: ThresholdViolationCreateDTO): Promise<ThresholdViolationDTO> =>
        api.create({ thresholdViolationCreateDTO }).then(r => r.data),

    update: (id: number, thresholdViolationUpdateDTO: ThresholdViolationUpdateDTO): Promise<ThresholdViolationDTO> =>
        api.update({ id, thresholdViolationUpdateDTO }).then(r => r.data),

    delete: (id: number): Promise<void> =>
        api._delete({ id }).then(() => undefined),
};
