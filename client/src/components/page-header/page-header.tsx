import { PropsWithChildren } from "react";

import styles from "./page-header.module.css";
import Box from "@mui/material/Box/Box";

interface Props extends PropsWithChildren {
  title: string;
}

export default function PageHeader(props: Props) {
  return (
    <Box className={ styles.page }>
      <h2>{ props.title }</h2>
      <hr></hr>
      <Box className={ styles.content }>
        {props.children}
      </Box>
    </Box>
  );
}