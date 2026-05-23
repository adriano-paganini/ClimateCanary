import React, { createContext, useContext, useEffect, useState } from 'react';
import { useUser } from './AuthenticatedUserContext';
import { DepartmentService } from '../services/DepartmentService';
import { DepartmentDTO } from '../generated-skeleton-api';

interface DepartmentContextType {
    departments: DepartmentDTO[];
    selectedDepartmentId: number | null;
    setSelectedDepartmentId: (id: number | null) => void;
    loading: boolean;
    error: string | null;
}

const DepartmentContext = createContext<DepartmentContextType | null>(null);

export function DepartmentProvider({ children }: { children: React.ReactNode }) {
    const { fullUser } = useUser();

    const [departments, setDepartments] = useState<DepartmentDTO[]>([]);
    const [selectedDepartmentId, setSelectedDepartmentId] = useState<number | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const userId = fullUser?.id;
        if (!userId) return;

        const load = async () => {
            try {
                const allDepts = await DepartmentService.getAll();
                const myDepartments = allDepts.filter(d => d.departmentLeadId === userId && d.id !== undefined);
                if (myDepartments.length === 0) {
                    setError('No department found for this department lead.');
                    return;
                }
                setDepartments(myDepartments);
                setSelectedDepartmentId(prev => {
                    // keep existing selection if it's still valid
                    if (prev && myDepartments.some(d => d.id === prev)) return prev;
                    return myDepartments[0].id ?? null;
                });
            } catch (err: unknown) {
                const msg = err instanceof Error ? err.message : String(err);
                setError(`Failed to load departments: ${msg}`);
            } finally {
                setLoading(false);
            }
        };

        void load();
    }, [fullUser?.id]);

    return (
        <DepartmentContext.Provider value={{ departments, selectedDepartmentId, setSelectedDepartmentId, loading, error }}>
            {children}
        </DepartmentContext.Provider>
    );
}

export function useDepartment() {
    const context = useContext(DepartmentContext);
    if (!context) {
        throw new Error('useDepartment must be used within a DepartmentProvider');
    }
    return context;
}
