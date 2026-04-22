import {
    ClimateHintControllerApi,
    ClimateHintCreateDTO,
    ClimateHintDTO,
    ClimateHintUpdateDTO,
} from "../generated-skeleton-api";
import { apiConfig } from "./apiConfig";

const api = new ClimateHintControllerApi(apiConfig);

export const ClimateHintService = {
    getAll: (): Promise<ClimateHintDTO[]> =>
        api.getClimateHints().then(r => r.data),

    getById: (id: number): Promise<ClimateHintDTO> =>
        api.getById6({ id }).then(r => r.data),

    create: (climateHintCreateDTO: ClimateHintCreateDTO): Promise<ClimateHintDTO> =>
        api.create6({ climateHintCreateDTO }).then(r => r.data),

    update: (id: number, climateHintUpdateDTO: ClimateHintUpdateDTO): Promise<ClimateHintDTO> =>
        api.update6({ id, climateHintUpdateDTO }).then(r => r.data),

    delete: (id: number): Promise<void> =>
        api.delete6({ id }).then(() => undefined),
};
