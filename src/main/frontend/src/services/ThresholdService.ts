import {
    ThresholdControllerApi,
    ThresholdCreateDTO,
    ThresholdDTO,
    ThresholdUpdateDTO,
} from "../generated-skeleton-api";
import { apiConfig } from "./apiConfig";

const api = new ThresholdControllerApi(apiConfig);

export const ThresholdService = {
    getAll: (): Promise<ThresholdDTO[]> =>
        api.getAll1().then(r => r.data),

    getById: (id: number): Promise<ThresholdDTO> =>
        api.getById1({ id }).then(r => r.data),

    create: (thresholdCreateDTO: ThresholdCreateDTO): Promise<ThresholdDTO> =>
        api.create1({ thresholdCreateDTO }).then(r => r.data),

    update: (id: number, thresholdUpdateDTO: ThresholdUpdateDTO): Promise<ThresholdDTO> =>
        api.update1({ id, thresholdUpdateDTO }).then(r => r.data),

    delete: (id: number): Promise<void> =>
        api.delete1({ id }).then(() => undefined),
};
