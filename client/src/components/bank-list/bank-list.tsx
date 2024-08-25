import { useState } from "react";

import Grid from '@mui/material/Grid';
import TextField from '@mui/material/TextField';

import Institution from '../../model/institution.model';
import BankCard from '../bank-card/bank-card'
import UserConsent from '../../model/user-consent.model';
import { Paper } from "@mui/material";

interface Props {
    institutions: Array<Institution> | undefined;
    userConsents: Array<UserConsent> | undefined;
    onLinkSelect?: (institution: Institution) => void;
}

function filter(filterText: string, text: string): boolean {
  return filterText.length === 0 || text.toLocaleLowerCase().indexOf(filterText.toLocaleLowerCase()) >= 0;
}

export default function BankList(props: Props) {
  const [ institutionFilter, setInstitutionFilter ] = useState<string>("");

  function getConsent(institution: Institution): UserConsent | undefined {
    return (props.userConsents) &&
     (props.userConsents.find(consent => consent.institutionId === institution.id));
  }

  return (
    <Grid container justifyContent="space-around" gap={ 2 }>
      <Grid item>
        <TextField id="institution-filter" label="Filter"
          value={ institutionFilter } onChange={ e => setInstitutionFilter(e.target.value) }
        />
      </Grid>
      <Grid item component={ Paper } padding={ 1 }
        minWidth={{ xs: "380px", sm: "500px" }}
        maxWidth={{ xs: "380px", sm: "500px" }}
        minHeight="36vh" maxHeight="36vh" overflow="auto">
        { props.institutions && props.institutions
          .filter( institution => filter(institutionFilter, institution.name))
          .sort((a, b) => a.name.localeCompare(b.name) )
          .map((institution, index: number) =>
            <BankCard key={ index } institution={ institution } consent={ getConsent(institution) } onLinkSelect= { props.onLinkSelect }/>
          )
        }
      </Grid>
    </Grid>
  );
}
