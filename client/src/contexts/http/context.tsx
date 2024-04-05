import { createContext, useContext, useReducer } from "react";

import HttpService from "../../services/http-common";

/**
 * A context that allows components to access the http service.
 */
const HttpContext = createContext(HttpService);
export function useHttp(): typeof HttpService {
  return useContext(HttpContext);
}

export default function HttpProvider(props: React.PropsWithChildren) {
  return (
    <HttpContext.Provider value={ HttpService }>
      {props.children}
    </HttpContext.Provider>
  );
}