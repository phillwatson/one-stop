import { createContext, useContext, useReducer } from "react";

import { Message, MessageAction } from "./model";
import MessageStack from "./message-stack";
import { AxiosError } from "axios";
import { ServiceError } from "../../model/service-error";

function extractMessages(error: AxiosError): Message[] {
  const response: any = error.response;
  if ((! response) || (!response.data)) {
    return [ { id: Date.now(), text: error.message, level: 'error' } ];
  }

  return response.data.errors.map((error: ServiceError, index: number) => {
    let message = error.message;

    // append context attributes - would be better to apply these to a message locale template
    if (error.contextAttributes) {
      for (const prop in error.contextAttributes) {
        let value = error.contextAttributes[prop];
        message = message + `\n${prop}: ${value}`;
      };
    }

    return { id: Date.now() + index, text: message, level: error.severity };
  });
}

/**
 * Accepts actions and updates the list of Messages accordingly.
 * 
 * @param messages the list of Messages to be updated (the state)
 * @param action the action to be performed on the Messages.
 * @returns the modified copy of the given Messages.
 */
function messageActionReducer(messages: Message[], action: MessageAction | AxiosError): Message[] {
  if (action instanceof AxiosError) {
    const errors = extractMessages(action);
    return [ ...errors, ...messages ];
  }

  switch (action.type) {
    case 'add': {
      return [ { id: Date.now(), text: action.text, level: action.level }, ...messages ];
    }

    case 'delete': {
      return messages.filter(e => e.id !== action.id);
    }

    default: {
      throw Error('Unknown message action: ' + action);
    }
  }
}

/**
 * A context that allows components to access the list of Messages, and respond
 * to changes to the list.
 */
const MessageContext = createContext(Array<Message>());
export function useMessages(): Message[] {
  return useContext(MessageContext);
}

/**
 * A context that allows components to dispatch MessageAction to update the list of
 * Messages.
 */
const MessageActionDispatchContext = createContext(function(action: MessageAction | AxiosError) {});
export function useMessageDispatch() {
  return useContext(MessageActionDispatchContext);
}

/**
 * A component that wraps child components within the context of a list of Messages.
 * 
 * @param props the child components to be wrapped.
 * @returns the MessageProvider component.
 */
export default function MessageProvider(props: React.PropsWithChildren) {
  const [messages, dispatch] = useReducer(messageActionReducer, []);

  return (
    <MessageContext.Provider value={messages}>
      <MessageActionDispatchContext.Provider value={dispatch}>
        { props.children }

        <MessageStack messages={messages} dispatch={dispatch} />
      </MessageActionDispatchContext.Provider>
    </MessageContext.Provider>
  );
}
