/**
 * This code is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
import React from "react";
import { InputText } from "primereact/inputtext";
import { Password } from "primereact/password";
import { MultiSelect } from "primereact/multiselect";
import { Dropdown } from "primereact/dropdown";
import { Message } from "primereact/message";
import { DepartmentDTO, RoomDTO, UserxCreateDTO, UserxDTO, UserxRole } from "../generated-skeleton-api";
import "../styles/UserForm.css";
import {hasUserRole, rolesToArray} from "../utilities/userxUtilities";

export const COUNTRY_CODE_OPTIONS: Array<{ label: string; value: string; search: string }> = [
    { label: "+93 Afghanistan", value: "+93", search: "Afghanistan +93" },
    { label: "+355 Albania", value: "+355", search: "Albania +355" },
    { label: "+213 Algeria", value: "+213", search: "Algeria +213" },
    { label: "+376 Andorra", value: "+376", search: "Andorra +376" },
    { label: "+244 Angola", value: "+244", search: "Angola +244" },
    { label: "+54 Argentina", value: "+54", search: "Argentina +54" },
    { label: "+374 Armenia", value: "+374", search: "Armenia +374" },
    { label: "+61 Australia", value: "+61", search: "Australia +61" },
    { label: "+43 Austria", value: "+43", search: "Austria +43" },
    { label: "+994 Azerbaijan", value: "+994", search: "Azerbaijan +994" },
    { label: "+973 Bahrain", value: "+973", search: "Bahrain +973" },
    { label: "+880 Bangladesh", value: "+880", search: "Bangladesh +880" },
    { label: "+375 Belarus", value: "+375", search: "Belarus +375" },
    { label: "+32 Belgium", value: "+32", search: "Belgium +32" },
    { label: "+591 Bolivia", value: "+591", search: "Bolivia +591" },
    { label: "+387 Bosnia", value: "+387", search: "Bosnia and Herzegovina +387" },
    { label: "+55 Brazil", value: "+55", search: "Brazil +55" },
    { label: "+359 Bulgaria", value: "+359", search: "Bulgaria +359" },
    { label: "+855 Cambodia", value: "+855", search: "Cambodia +855" },
    { label: "+237 Cameroon", value: "+237", search: "Cameroon +237" },
    { label: "+1 Canada", value: "+1", search: "Canada United States +1" },
    { label: "+56 Chile", value: "+56", search: "Chile +56" },
    { label: "+86 China", value: "+86", search: "China +86" },
    { label: "+57 Colombia", value: "+57", search: "Colombia +57" },
    { label: "+385 Croatia", value: "+385", search: "Croatia +385" },
    { label: "+357 Cyprus", value: "+357", search: "Cyprus +357" },
    { label: "+420 Czechia", value: "+420", search: "Czech Republic Czechia +420" },
    { label: "+45 Denmark", value: "+45", search: "Denmark +45" },
    { label: "+20 Egypt", value: "+20", search: "Egypt +20" },
    { label: "+372 Estonia", value: "+372", search: "Estonia +372" },
    { label: "+358 Finland", value: "+358", search: "Finland +358" },
    { label: "+33 France", value: "+33", search: "France +33" },
    { label: "+995 Georgia", value: "+995", search: "Georgia +995" },
    { label: "+49 Germany", value: "+49", search: "Germany +49" },
    { label: "+30 Greece", value: "+30", search: "Greece +30" },
    { label: "+36 Hungary", value: "+36", search: "Hungary +36" },
    { label: "+354 Iceland", value: "+354", search: "Iceland +354" },
    { label: "+91 India", value: "+91", search: "India +91" },
    { label: "+62 Indonesia", value: "+62", search: "Indonesia +62" },
    { label: "+353 Ireland", value: "+353", search: "Ireland +353" },
    { label: "+972 Israel", value: "+972", search: "Israel +972" },
    { label: "+39 Italy", value: "+39", search: "Italy +39" },
    { label: "+81 Japan", value: "+81", search: "Japan +81" },
    { label: "+962 Jordan", value: "+962", search: "Jordan +962" },
    { label: "+7 Kazakhstan", value: "+7", search: "Kazakhstan Russia +7" },
    { label: "+254 Kenya", value: "+254", search: "Kenya +254" },
    { label: "+965 Kuwait", value: "+965", search: "Kuwait +965" },
    { label: "+371 Latvia", value: "+371", search: "Latvia +371" },
    { label: "+961 Lebanon", value: "+961", search: "Lebanon +961" },
    { label: "+423 Liechtenstein", value: "+423", search: "Liechtenstein +423" },
    { label: "+370 Lithuania", value: "+370", search: "Lithuania +370" },
    { label: "+352 Luxembourg", value: "+352", search: "Luxembourg +352" },
    { label: "+60 Malaysia", value: "+60", search: "Malaysia +60" },
    { label: "+356 Malta", value: "+356", search: "Malta +356" },
    { label: "+52 Mexico", value: "+52", search: "Mexico +52" },
    { label: "+373 Moldova", value: "+373", search: "Moldova +373" },
    { label: "+377 Monaco", value: "+377", search: "Monaco +377" },
    { label: "+212 Morocco", value: "+212", search: "Morocco +212" },
    { label: "+31 Netherlands", value: "+31", search: "Netherlands +31" },
    { label: "+64 New Zealand", value: "+64", search: "New Zealand +64" },
    { label: "+234 Nigeria", value: "+234", search: "Nigeria +234" },
    { label: "+47 Norway", value: "+47", search: "Norway +47" },
    { label: "+968 Oman", value: "+968", search: "Oman +968" },
    { label: "+92 Pakistan", value: "+92", search: "Pakistan +92" },
    { label: "+51 Peru", value: "+51", search: "Peru +51" },
    { label: "+63 Philippines", value: "+63", search: "Philippines +63" },
    { label: "+48 Poland", value: "+48", search: "Poland +48" },
    { label: "+351 Portugal", value: "+351", search: "Portugal +351" },
    { label: "+974 Qatar", value: "+974", search: "Qatar +974" },
    { label: "+40 Romania", value: "+40", search: "Romania +40" },
    { label: "+7 Russia", value: "+7", search: "Russia Kazakhstan +7" },
    { label: "+966 Saudi Arabia", value: "+966", search: "Saudi Arabia +966" },
    { label: "+381 Serbia", value: "+381", search: "Serbia +381" },
    { label: "+65 Singapore", value: "+65", search: "Singapore +65" },
    { label: "+421 Slovakia", value: "+421", search: "Slovakia +421" },
    { label: "+386 Slovenia", value: "+386", search: "Slovenia +386" },
    { label: "+27 South Africa", value: "+27", search: "South Africa +27" },
    { label: "+82 South Korea", value: "+82", search: "South Korea +82" },
    { label: "+34 Spain", value: "+34", search: "Spain +34" },
    { label: "+46 Sweden", value: "+46", search: "Sweden +46" },
    { label: "+41 Switzerland", value: "+41", search: "Switzerland +41" },
    { label: "+886 Taiwan", value: "+886", search: "Taiwan +886" },
    { label: "+66 Thailand", value: "+66", search: "Thailand +66" },
    { label: "+216 Tunisia", value: "+216", search: "Tunisia +216" },
    { label: "+90 Turkey", value: "+90", search: "Turkey +90" },
    { label: "+380 Ukraine", value: "+380", search: "Ukraine +380" },
    { label: "+971 UAE", value: "+971", search: "United Arab Emirates UAE +971" },
    { label: "+44 United Kingdom", value: "+44", search: "United Kingdom UK +44" },
    { label: "+1 United States", value: "+1", search: "United States USA Canada +1" },
    { label: "+598 Uruguay", value: "+598", search: "Uruguay +598" },
    { label: "+998 Uzbekistan", value: "+998", search: "Uzbekistan +998" },
    { label: "+58 Venezuela", value: "+58", search: "Venezuela +58" },
    { label: "+84 Vietnam", value: "+84", search: "Vietnam +84" },
];

type UserFormFieldErrors = Partial<Record<keyof UserxCreateDTO | keyof UserxDTO | 'departmentId' | 'roomId', string>>;

interface UserFormProps {
    user: UserxDTO | UserxCreateDTO;
    isNewUser: boolean;
    fieldErrors?: UserFormFieldErrors;
    onInputChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
    onRolesChange: (event: { value: string[] }) => void;
    onPhoneChange: (phone: string) => void;
    disableUsername?: boolean;
    departments?: DepartmentDTO[];
    rooms?: RoomDTO[];
    selectedDepartmentId?: number;
    selectedRoomId?: number;
    onDepartmentChange?: (departmentId?: number) => void;
    onRoomChange?: (roomId?: number) => void;
    roomsLoading?: boolean;
    lockedRoles?: UserxRole[];
}

/**
 * Form for creating or editing a user.
 * @param user the user to be edited
 * @param isNewUser describes if the user is new, i.e., created
 * @param fieldErrors field validation
 * @param onInputChange callback when the input changes
 * @param onRolesChange callback when the roles change
 * @param onPhoneChange callback when the phone number changes
 * @param disableUsername if true, the username cannot be changed
 */
const UserForm: React.FC<UserFormProps> =
    ({
         user,
         isNewUser,
         fieldErrors,
         onInputChange,
         onRolesChange,
         onPhoneChange,
         disableUsername = false,
         departments = [],
         rooms = [],
         selectedDepartmentId,
         selectedRoomId,
         onDepartmentChange,
         onRoomChange,
         roomsLoading = false,
         lockedRoles = [],
     }) => {
        const allowedRoles = [
            UserxRole.SYSTEM_ADMIN,
            UserxRole.BUILDING_ADMIN,
            UserxRole.MANAGEMENT,
            UserxRole.EMPLOYEE,
        ];
        const userRoles = allowedRoles.map(role => ({ label: role, value: role }));
        const selectedRoles = Array.from(new Set([...rolesToArray(user.roles), ...lockedRoles]));
        const isEmployee = hasUserRole(user, UserxRole.EMPLOYEE) || lockedRoles.includes(UserxRole.EMPLOYEE);
        const departmentOptions = departments
            .filter(department => department.id !== undefined)
            .map(department => ({ label: department.name ?? `Department ${department.id}`, value: department.id }));
        const roomOptions = rooms
            .filter(room => room.id !== undefined)
            .map(room => ({ label: room.name ?? `Room ${room.id}`, value: room.id }));
        const normalizedPhone = user.phone ?? "";
        const selectedCountry = COUNTRY_CODE_OPTIONS.find(option => normalizedPhone.startsWith(option.value))?.value ?? "+43";
        const localPhone = normalizedPhone.startsWith(selectedCountry)
            ? normalizedPhone.slice(selectedCountry.length).replace(/\s/g, "")
            : normalizedPhone.replace(/^\+\d+\s*/, "").replace(/\s/g, "");

        const updatePhone = (countryCode: string, localNumber: string) => {
            onPhoneChange(localNumber ? `${countryCode} ${localNumber}` : countryCode);
        };

        return (
            <div>
                <h1>{user.firstName} {user.lastName}</h1>
                <h5><span className="p-text-secondary">{user.username}</span></h5>

                {isNewUser && (
                    <Message
                        severity="warn"
                        text="Choose the username carefully. It cannot be changed after the user is created."
                        className="mb-3"
                    />
                )}

                {/* create form */}
                <form>
                    <div className="card p-fluid flex flex-wrap gap-3">
                        <div className="flex-auto mb-3">
                            <label htmlFor="username" className="font-bold block">Username</label>
                            <InputText
                                id="username"
                                name="username"
                                value={user.username}
                                onChange={onInputChange}
                                required={true}
                                placeholder="Username"
                                autoComplete="off"
                                disabled={disableUsername}
                                className={fieldErrors?.username ? "p-invalid" : undefined}
                            />
                            {disableUsername && (
                                <small style={{ display: "block", color: "#64748b" }}>
                                    Usernames cannot be changed after creation.
                                </small>
                            )}
                            {fieldErrors?.username && <small className="p-error">{fieldErrors.username}</small>}
                        </div>

                        <div className="flex-auto mb-3">
                            <label htmlFor="firstName" className="font-bold block">First Name</label>
                            <InputText
                                id="firstName"
                                name="firstName"
                                value={user.firstName}
                                onChange={onInputChange}
                                placeholder="First Name"
                                autoComplete="off"
                                className={fieldErrors?.firstName ? "p-invalid" : undefined}
                            />
                            {fieldErrors?.firstName && <small className="p-error">{fieldErrors.firstName}</small>}
                        </div>

                        <div className="flex-auto mb-3">
                            <label htmlFor="lastName" className="font-bold block">Last Name</label>
                            <InputText
                                id="lastName"
                                name="lastName"
                                value={user.lastName}
                                onChange={onInputChange}
                                placeholder="Last Name"
                                autoComplete="off"
                                className={fieldErrors?.lastName ? "p-invalid" : undefined}
                            />
                            {fieldErrors?.lastName && <small className="p-error">{fieldErrors.lastName}</small>}
                        </div>

                        <div className="flex-auto mb-3">
                            <label htmlFor="email" className="font-bold block">E-Mail</label>
                            <InputText
                                id="email"
                                name="email"
                                value={user.email ?? ""}
                                onChange={onInputChange}
                                placeholder="E-Mail"
                                autoComplete="off"
                                className={fieldErrors?.email ? "p-invalid" : undefined}
                            />
                            {fieldErrors?.email && <small className="p-error">{fieldErrors.email}</small>}
                        </div>

                        {isNewUser && (
                            <div className="flex-auto mb-3">
                                <label htmlFor="password" className="font-bold block">Password</label>
                                <Password
                                    inputId="password"
                                    name="password"
                                    value={(user as UserxCreateDTO).password ?? ""}
                                    onChange={onInputChange}
                                    placeholder="Password"
                                    autoComplete="off"
                                    inputClassName={fieldErrors?.password ? "p-invalid" : undefined}
                                />
                                {fieldErrors?.password && <small className="p-error">{fieldErrors.password}</small>}
                            </div>
                        )}

                        <div className="flex-auto mb-3">
                            <label htmlFor="roles" className="font-bold block">Roles</label>
                            <MultiSelect
                                inputId="roles"
                                name="roles"
                                value={selectedRoles}
                                onChange={(event) => onRolesChange({
                                    value: Array.from(new Set([...(event.value as string[]), ...lockedRoles])),
                                })}
                                options={userRoles}
                                optionLabel="label"
                                placeholder="Select Roles"
                                className="w-full md:w-20rem"
                                appendTo="self"
                                panelClassName="user-form-overlay-panel"
                                scrollHeight="14rem"
                                invalid={!!fieldErrors?.roles}
                            />
                            {lockedRoles.length > 0 && (
                                <small style={{ display: "block", color: "#64748b" }}>
                                    Required roles cannot be removed here.
                                </small>
                            )}
                            {fieldErrors?.roles && <small className="p-error">{fieldErrors.roles}</small>}
                        </div>

                        {isEmployee && (
                            <>
                                <div className="flex-auto mb-3">
                                    <label htmlFor="departmentId" className="font-bold block">Department</label>
                                    <Dropdown
                                        inputId="departmentId"
                                        value={selectedDepartmentId}
                                        options={departmentOptions}
                                        onChange={(event) => onDepartmentChange?.(event.value)}
                                        placeholder="Select Department"
                                        className={fieldErrors?.departmentId ? "p-invalid" : undefined}
                                        appendTo="self"
                                        panelClassName="user-form-overlay-panel"
                                    />
                                    {fieldErrors?.departmentId && <small className="p-error">{fieldErrors.departmentId}</small>}
                                </div>

                                <div className="flex-auto mb-3">
                                    <label htmlFor="roomId" className="font-bold block">Room</label>
                                    <Dropdown
                                        inputId="roomId"
                                        value={selectedRoomId}
                                        options={roomOptions}
                                        onChange={(event) => onRoomChange?.(event.value)}
                                        placeholder={selectedDepartmentId ? "Select Room" : "Select Department first"}
                                        disabled={!selectedDepartmentId || roomsLoading}
                                        loading={roomsLoading}
                                        className={fieldErrors?.roomId ? "p-invalid" : undefined}
                                        appendTo="self"
                                        panelClassName="user-form-overlay-panel"
                                    />
                                    {fieldErrors?.roomId && <small className="p-error">{fieldErrors.roomId}</small>}
                                </div>
                            </>
                        )}

                        <div className="flex-auto mb-3">
                            <label htmlFor="phone" className="font-bold block">Phone</label>
                            <div style={{ display: "grid", gridTemplateColumns: "12rem minmax(10rem, 1fr)", gap: "0.5rem" }}>
                                <Dropdown
                                    inputId="phoneCountry"
                                    value={selectedCountry}
                                    options={COUNTRY_CODE_OPTIONS}
                                    onChange={(event) => updatePhone(event.value, localPhone)}
                                    filter
                                    optionLabel="label"
                                    filterBy="search,label,value"
                                    appendTo="self"
                                    panelClassName="user-form-overlay-panel"
                                    scrollHeight="14rem"
                                    className={fieldErrors?.phone ? "p-invalid" : undefined}
                                />
                                <InputText
                                    id="phone"
                                    name="phone"
                                    value={localPhone}
                                    onChange={(event) => updatePhone(selectedCountry, event.target.value.replace(/\D/g, ""))}
                                    placeholder="123456789"
                                    autoComplete="off"
                                    keyfilter="int"
                                    className={fieldErrors?.phone ? "p-invalid" : undefined}
                                />
                            </div>
                            {fieldErrors?.phone && <small className="p-error">{fieldErrors.phone}</small>}
                        </div>
                    </div>
                </form>
            </div>
        );
    };

export default UserForm;
