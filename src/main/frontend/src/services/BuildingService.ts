import {
    BuildingControllerApi,
    BuildingCreateDTO,
    BuildingDTO,
    BuildingUpdateDTO,
    RoomDTO,
} from "../generated-skeleton-api";
import { apiConfig } from "./apiConfig";

const api = new BuildingControllerApi(apiConfig);

export const BuildingService = {
    getAll: (): Promise<BuildingDTO[]> =>
        api.getAll5().then(r => r.data),

    getById: (id: number): Promise<BuildingDTO> =>
        api.getById7({ id }).then(r => r.data),

    getRooms: (id: number): Promise<RoomDTO[]> =>
        api.getRooms1({ id }).then(r => r.data),

    create: (buildingCreateDTO: BuildingCreateDTO): Promise<BuildingDTO> =>
        api.create7({ buildingCreateDTO }).then(r => r.data),

    update: (id: number, buildingUpdateDTO: BuildingUpdateDTO): Promise<BuildingDTO> =>
        api.update7({ id, buildingUpdateDTO }).then(r => r.data),

    delete: (id: number): Promise<void> =>
        api.delete7({ id }).then(() => undefined),
};
