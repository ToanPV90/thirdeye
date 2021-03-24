import { ReactNode } from "react";

export interface AuthProviderProps {
    children: ReactNode;
}

export interface AuthContextProps {
    authDisabled: boolean;
    authenticated: boolean;
    accessToken: string;
    signIn: () => Promise<boolean>;
    signOut: () => void;
}
