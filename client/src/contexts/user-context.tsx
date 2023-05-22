import { PropsWithChildren, createContext, useContext, useEffect, useState } from "react";
import UserProfile from "../model/user-profile.model";
import ProfileService from '../services/profile.service'

// /**
//  * An interface to describe the state that we will pass in the provider.
//  */
// interface UserProfileContextValue {
//   user: UserProfile | undefined;
//   setUser: Dispatch<SetStateAction<UserProfile | undefined>>;
// }

/**
 * The context that will capture the state and the update method.
 */
const UserContext = createContext<UserProfile | undefined>(undefined);
export function useCurrentUser(): UserProfile | undefined {
  return useContext(UserContext);
}

const SetUserContext = createContext(function(user: UserProfile | undefined) {});
export function useSetCurrentUser() {
  return useContext(SetUserContext);
}

export default function UserProfileProvider(props: PropsWithChildren) {
  // create the user-profile state and update method
  const [user, setUser] = useState<UserProfile | undefined>();

  // pre-load the user profile
  useEffect(() => {
    ProfileService.get()
      .then((response) => { setUser(response.data); } )
      .catch(() => setUser(undefined));
  }, []);

  return (
    <UserContext.Provider value={user}>
      <SetUserContext.Provider value={setUser}>
        { props.children }
      </SetUserContext.Provider>
    </UserContext.Provider>
  );
}
