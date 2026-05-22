import globalAxios from "axios";
import {
    SensorStationControllerApi,
    SensorStationCreateDTO,
    SensorStationDTO,
    SensorStationUpdateDTO,
} from "../generated-skeleton-api";
import { apiConfig } from "./apiConfig";

const api = new SensorStationControllerApi(apiConfig);

export const SensorStationService = {
    getAll: (): Promise<SensorStationDTO[]> =>
        api.getAll2().then(r => r.data),

    getById: (id: number): Promise<SensorStationDTO> =>
        api.getById2({ id }).then(r => r.data),

    create: (sensorStationCreateDTO: SensorStationCreateDTO): Promise<SensorStationDTO> =>
        api.create2({ sensorStationCreateDTO }).then(r => r.data),

    update: (id: number, sensorStationUpdateDTO: SensorStationUpdateDTO): Promise<SensorStationDTO> =>
        api.update2({ id, sensorStationUpdateDTO }).then(r => r.data),

    delete: (id: number): Promise<void> =>
        api.delete2({ id }).then(() => undefined),

    updateSetup: (id: number, measurementInterval: number, sensorStationUpdateDTO: SensorStationUpdateDTO): Promise<SensorStationDTO> =>
        globalAxios.patch<SensorStationDTO>(`/api/sensorstation/${id}/${measurementInterval}`, sensorStationUpdateDTO).then(r => r.data),

    triggerScan: (piId: number): Promise<void> =>
        globalAxios.post(`/api/sensorstation/find/${piId}`).then(() => undefined),

    getAvailableForPi: (piId: number): Promise<SensorStationDTO[]> =>
        globalAxios.get<SensorStationDTO[]>(`/api/bpi/${piId}/availablesensorstations`).then(r => r.data),
};
