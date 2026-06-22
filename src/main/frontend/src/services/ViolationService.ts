import {
    GetAllViolationStatusEnum,
    ThresholdViolationControllerApi,
    ThresholdViolationCreateDTO,
    ThresholdViolationDTO,
    ThresholdViolationDTOViolationStatusEnum,
    ThresholdViolationUpdateDTO,
    ThresholdViolationUpdateDTOViolationStatusEnum,
} from "../generated-skeleton-api";
import { apiConfig } from "./apiConfig";

const api = new ThresholdViolationControllerApi(apiConfig);

export { GetAllViolationStatusEnum as ViolationStatusEnum };

export const ViolationService = {
    getAll: (params: {
        violationStatus?: GetAllViolationStatusEnum;
        roomId?: number;
        departmentId?: number;
    } = {}): Promise<ThresholdViolationDTO[]> =>
        api.getAll(params).then(r => r.data),

    getById: (id: number): Promise<ThresholdViolationDTO> =>
        api.getById({ id }).then(r => r.data),

    create: (thresholdViolationCreateDTO: ThresholdViolationCreateDTO): Promise<ThresholdViolationDTO> =>
        api.create({ thresholdViolationCreateDTO }).then(r => r.data),

    update: (id: number, thresholdViolationUpdateDTO: ThresholdViolationUpdateDTO): Promise<ThresholdViolationDTO> =>
        api.update({ id, thresholdViolationUpdateDTO }).then(r => r.data),

    delete: (id: number): Promise<void> =>
        api._delete({ id }).then(() => undefined),

    disable: (id: number): Promise<ThresholdViolationDTO> =>
        api.update({
            id,
            thresholdViolationUpdateDTO: {
                violationStatus: ThresholdViolationDTOViolationStatusEnum.DISABLED as unknown as ThresholdViolationUpdateDTOViolationStatusEnum,
            },
        }).then(r => r.data),
};
