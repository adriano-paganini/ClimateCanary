import {
    AddressControllerApi,
    AddressCreateDTO,
    AddressDTO,
    AddressUpdateDTO,
} from "../generated-skeleton-api";
import { apiConfig } from "./apiConfig";

const api = new AddressControllerApi(apiConfig);

export const AddressService = {
    getAll: (): Promise<AddressDTO[]> =>
        api.getAll7().then(r => r.data),

    getById: (id: number): Promise<AddressDTO> =>
        api.getById9({ id }).then(r => r.data),

    create: (addressCreateDTO: AddressCreateDTO): Promise<AddressDTO> =>
        api.create9({ addressCreateDTO }).then(r => r.data),

    update: (id: number, addressUpdateDTO: AddressUpdateDTO): Promise<AddressDTO> =>
        api.update9({ id, addressUpdateDTO }).then(r => r.data),

    delete: (id: number): Promise<void> =>
        api.delete9({ id }).then(() => undefined),
};
