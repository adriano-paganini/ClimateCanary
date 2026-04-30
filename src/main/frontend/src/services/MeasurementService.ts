import {
    GetAll9MetricEnum,
    MeasurementControllerApi,
    MeasurementDTO,
} from "../generated-skeleton-api";
import { apiConfig } from "./apiConfig";

const api = new MeasurementControllerApi(apiConfig);

export { GetAll9MetricEnum as MetricEnum };

export const MeasurementService = {
    getAll: (params: {
        roomId?: number;
        metric?: GetAll9MetricEnum;
        from?: string;
        to?: string;
    } = {}): Promise<MeasurementDTO[]> =>
        api.getAll9(params).then(r => r.data),

    getById: (id: number): Promise<MeasurementDTO> =>
        api.getById11({ id }).then(r => r.data),

    getLatestPerMetric: (roomId: number): Promise<Record<string, MeasurementDTO>> =>
        api.getLatestPerMetric({ roomId }).then(r => r.data),
};
