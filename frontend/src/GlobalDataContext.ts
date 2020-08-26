import React from "react";

export const GlobalDataContext = React.createContext({
    domain: '',
    user: {
        name: '',
        alliance: '',
    },
});
