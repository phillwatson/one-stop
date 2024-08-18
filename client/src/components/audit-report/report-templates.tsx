import { useEffect, useState } from 'react';

import Grid from '@mui/material/Grid';
import FormControl from '@mui/material/FormControl';
import InputLabel from "@mui/material/InputLabel";
import Select from "@mui/material/Select";
import MenuItem from "@mui/material/MenuItem";
import TextField from '@mui/material/TextField';

import { AuditReportTemplate } from "../../model/audit-report.model";


interface Props {
  required?: boolean;
  templates: Array<AuditReportTemplate>;
  templateName?: string;
  onSelected?: (template: AuditReportTemplate) => void;
}

export function AuditReportTemplates(props: Props) {
  const [ selectedTemplate, setSelectedTemplate ] = useState<AuditReportTemplate>();

  useEffect(() => {
    if (props.templateName) {
      const template = props.templates.find(template => template.name === props.templateName);
      setSelectedTemplate(template);
    }
  }, [ props.templateName, props.templates ]);

  function selectTemplate(event: any) {
    const templateName = event.target.value;
    const template = props.templates.find(template => template.name === templateName)
    setSelectedTemplate(template);

    if ((template) && (props.onSelected)) {
      props.onSelected(template);
    }
  }

  return (
    <Grid container direction="column" rowGap={1}>
      <Grid container direction="row">
        <FormControl fullWidth required = { props.required }>
          <InputLabel id="select-template">Report Template</InputLabel>
          <Select labelId="select-template" label="Report Template" value={ selectedTemplate?.name || "" } onChange={ selectTemplate }>
            { props.templates.map(template =>
              <MenuItem key={ template.name } value={ template.name }>{ template.name }</MenuItem>
            )}
          </Select>
        </FormControl>
      </Grid>
      <Grid container direction="row">
        <TextField id="template-description" value={ selectedTemplate?.description }
          fullWidth multiline rows={ 4 } variant="filled" disabled
        />
      </Grid>
    </Grid>
  );
}