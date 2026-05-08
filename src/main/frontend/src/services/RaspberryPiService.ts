import globalAxios from "axios";
import {
    RaspberryPiControllerApi,
    RaspberryPiCreateDTO,
    RaspberryPiDTO,
    RaspberryPiUpdateDTO,
    SensorStationDTO,
} from "../generated-skeleton-api";
import { apiConfig } from "./apiConfig";

const api = new RaspberryPiControllerApi(apiConfig);

export const RaspberryPiService = {
    getAll: (): Promise<RaspberryPiDTO[]> =>
        api.getAll6().then(r => r.data),

    getById: (id: number): Promise<RaspberryPiDTO> =>
        api.getById8({ id }).then(r => r.data),

    getSensorStations: (id: number): Promise<SensorStationDTO[]> =>
        api.getSensorStations({ id }).then(r => r.data),

    getAvailableSensorStations: (id: number): Promise<SensorStationDTO[]> =>
        globalAxios.get<SensorStationDTO[]>(`/api/bpi/${id}/availablesensorstations`).then(r => r.data),

    getConfig: (piId: number): Promise<string> =>
        globalAxios.get<string>(`/api/cpi/${piId}/config`, { responseType: 'text' }).then(r => r.data),

    create: (raspberryPiCreateDTO: RaspberryPiCreateDTO): Promise<RaspberryPiDTO> =>
        api.create8({ raspberryPiCreateDTO }).then(r => r.data),

    update: (id: number, raspberryPiUpdateDTO: RaspberryPiUpdateDTO): Promise<RaspberryPiDTO> =>
        api.update8({ id, raspberryPiUpdateDTO }).then(r => r.data),

    delete: (id: number): Promise<void> =>
        api.delete8({ id }).then(() => undefined),
};
