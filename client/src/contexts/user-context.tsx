import { PropsWithChildren, createContext, useContext, useEffect, useState } from "react";
import UserProfile from "../model/user-profile.model";
import ProfileService from '../services/profile.service'

// /**
//  * An interface to describe the state that we will pass in the provider.
//  */
interface UserProfileContextValue {
  user: UserProfile | undefined;
  setUser: (user: UserProfile | undefined) => void;
}

/**
 * The context that will capture the state and the update method.
 */
const UserContext = createContext<UserProfileContextValue>({ user: undefined, setUser: (user: UserProfile | undefined) => {} });
export function useCurrentUser(): [UserProfile | undefined, (user: UserProfile | undefined) => void] {
  const x = useContext(UserContext);
  return [ x.user, x.setUser ];
}

export default function UserProfileProvider(props: PropsWithChildren) {
  // create the user-profile state and update method
  const [user, setUser] = useState<UserProfile | undefined>();

  // pre-load the user profile
  useEffect(() => {
    ProfileService.get()
      .then(user => { setUser(user); } )
      .catch(() => setUser(undefined));
  }, []);

  return (
    <UserContext.Provider value={ { user, setUser }}>
        { props.children }
    </UserContext.Provider>
  );
}
