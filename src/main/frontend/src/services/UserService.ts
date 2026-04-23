import {
    AbsenceDTO,
    AdminControllerApi,
    UserxControllerApi,
    UserxCreateDTO,
    UserxDTO,
    UserxSelfUpdateDTO,
    UserxUpdateDTO,
} from "../generated-skeleton-api";
import { apiConfig } from "./apiConfig";

const adminApi = new AdminControllerApi(apiConfig);
const userxApi = new UserxControllerApi(apiConfig);

export const UserService = {
    // Admin operations
    getAllUsers: (): Promise<UserxDTO[]> =>
        adminApi.getAllUsers().then(r => r.data),

    getUser: (id: number): Promise<UserxDTO> =>
        adminApi.getUser({ id }).then(r => r.data),

    createUser: (userxCreateDTO: UserxCreateDTO): Promise<UserxDTO> =>
        adminApi.createUser({ userxCreateDTO }).then(r => r.data),

    updateUser: (id: number, userxUpdateDTO: UserxUpdateDTO): Promise<UserxDTO> =>
        adminApi.updateUser({ id, userxUpdateDTO }).then(r => r.data),

    deleteUser: (id: number): Promise<void> =>
        adminApi.deleteUser({ id }).then(() => undefined),

    // Current-user self-service operations
    getCurrentUser: (): Promise<UserxDTO> =>
        userxApi.getCurrentUser().then(r => r.data),

    getCurrentUserAbsences: (): Promise<AbsenceDTO[]> =>
        userxApi.getCurrentUserAbsences().then(r => r.data),

    updateCurrentUser: (userxSelfUpdateDTO: UserxSelfUpdateDTO): Promise<UserxDTO> =>
        userxApi.updateCurrentUser({ userxSelfUpdateDTO }).then(r => r.data),

    deleteCurrentUser: (): Promise<void> =>
        userxApi.deleteCurrentUser().then(() => undefined),
};
