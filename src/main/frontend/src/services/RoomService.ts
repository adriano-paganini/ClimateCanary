import {
    RoomControllerApi,
    RoomCreateDTO,
    RoomDTO,
    RoomUpdateDTO,
} from "../generated-skeleton-api";
import { apiConfig } from "./apiConfig";

const api = new RoomControllerApi(apiConfig);

export const RoomService = {
    getAll: (): Promise<RoomDTO[]> =>
        api.getAll3().then(r => r.data),

    getById: (id: number): Promise<RoomDTO> =>
        api.getById3({ id }).then(r => r.data),

    create: (roomCreateDTO: RoomCreateDTO): Promise<RoomDTO> =>
        api.create3({ roomCreateDTO }).then(r => r.data),

    update: (id: number, roomUpdateDTO: RoomUpdateDTO): Promise<RoomDTO> =>
        api.update3({ id, roomUpdateDTO }).then(r => r.data),

    delete: (id: number): Promise<void> =>
        api.delete3({ id }).then(() => undefined),
};
