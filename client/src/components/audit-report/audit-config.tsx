import { useEffect, useState } from 'react';

import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import Switch from '@mui/material/Switch';
import FormControlLabel from '@mui/material/FormControlLabel';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';

import { useMessageDispatch } from "../../contexts/messages/context";
import AuditReportService from "../../services/audit-report.service";
import AccountService from "../../services/account.service";
import CategoryService from "../../services/category.service";
import { Category } from '../../model/category.model';

import { AuditReportConfig, AuditReportTemplate, NULL_REPORT_CONFIG } from '../../model/audit-report.model';
import { AuditReportTemplates } from './report-templates';
import { InfoPopover } from '../info/info-popover';

interface Props {
  reportConfig?: AuditReportConfig;
  onSave?: (config: AuditReportConfig) => void;
}

const sourceDescriptions = {
  'ALL': 'All Accounts',
  'ACCOUNT': 'Selected Account',
  'CATEGORY_GROUP': 'Selected Category Group',
  'CATEGORY': 'Selected Category'
}

function Qualifier(props: { text: string }) {
  return <span style={{ fontSize: 'small' }}>{ props.text }</span>
}

function validateForm(config: AuditReportConfig): Array<string> {
  const errors = Array<string>();

  if (! config.templateName) {
    errors.push("Report Template is required");
  }

  if (config.name?.length === 0) {
    errors.push("Name is required");
  }

  if (config.source?.length === 0) {
    errors.push("Transaction Source is required");
  }

  if (config.source !== 'ALL' && !config.sourceId) {
    errors.push("Source Id is required");
  }

  return errors;
}

export default function EditAuditReportConfig(props: Props) {
  const showMessage = useMessageDispatch();

  const [ reportTemplates, setReportTemplates ] = useState<Array<AuditReportTemplate>>([]);
  const [ selectedTemplate, setSelectedTemplate ] = useState<AuditReportTemplate>();

  const [ accounts, setAccounts ] = useState<Array<any>>([]);
  const [ categoryGroups, setCategoryGroups ] = useState<Array<any>>([]);
  const [ categories, setCategories ] = useState<Array<any>>([]);

  useEffect(() => {
    AuditReportService.fetchAllTemplates()
      .then(response => setReportTemplates(response))
      .catch(err => showMessage(err));

    AccountService.fetchAll()
      .then(response => setAccounts(response.map(account =>
        <MenuItem key={ account.id } value={ account.id }><Qualifier text={ account.institution.name + ": "}/>{ account.name }</MenuItem>
      )))
      .catch(err => showMessage(err));

    CategoryService.fetchAllGroups()
      .then(response => {
        const groups = response.map(group => ({ id: group.id, name: group.name }));

        setCategoryGroups(groups.map(group => 
          <MenuItem key={ group.id } value={ group.id }>{ group.name }</MenuItem>
        ))

        // Fetch all categories for all groups
        Promise.all(
          groups.map(group => CategoryService.fetchAllCategories(group.id!))
        ).then((response: Category[][]) =>
          setCategories(response.flatMap(categoryList =>
            categoryList.map(category => {
              const group = groups.find(g => g.id! === category.groupId);
              return (
                <MenuItem key={ category.id } value={ category.id }>
                  <Qualifier text={ group!.name + ": "}/>{ category.name }
                </MenuItem>
              );
            })
          ))
        );
      })
      .catch(err => showMessage(err));
  }, [ showMessage]);

  const [ reportConfig, setReportConfig ] = useState<AuditReportConfig>(NULL_REPORT_CONFIG);
  useEffect(() => {
    if (props.reportConfig) {
      setReportConfig({ ...props.reportConfig });

      if (props.reportConfig.templateName) {
        setSelectedTemplate(reportTemplates.find(t => t.name === props.reportConfig!.templateName));
      }
    }
  }, [ props.reportConfig, reportTemplates ]);

  const [ reportSources, setReportSources ] = useState<any[]>([]);
  useEffect(() => {
    switch (reportConfig.source) {
      case 'ACCOUNT':
        setReportSources(accounts);
        break;

      case 'CATEGORY_GROUP':
        setReportSources(categoryGroups);
        break;

      case 'CATEGORY':
        setReportSources(categories);
        break;

      default:
        setReportSources([]);
    }
  }, [ reportConfig.source, showMessage, accounts, categoryGroups, categories ]);

  function handleSubmit(event: any) {
    event.preventDefault();

    validateForm(reportConfig).forEach(value => showMessage({ type: 'add', level: 'error', text: value}))
  }

  function handleTemplateSelected(template: AuditReportTemplate) {
    setSelectedTemplate(template);
    setReportConfig({ ...reportConfig, templateName: template.name });
  }

  function selectSourceType(event: any) {
    event.preventDefault();

    const source = event.target.value;
    setReportConfig({ ...reportConfig, source: source, sourceId: undefined });
  }

  function selectSourceId(event: any) {
    event.preventDefault();

    const sourceId = event.target.value;
    setReportConfig({ ...reportConfig, sourceId: sourceId });
  }

  return (
    <form onSubmit={ handleSubmit }>
      <FormControl fullWidth margin="normal">
        <FormControlLabel label="Disabled" control={
          <Switch id="disabled" checked={ reportConfig.disabled || false}
            onChange={ e => setReportConfig({...reportConfig, disabled: e.target.checked}) }/>
        } />
      </FormControl>

      <Box component="fieldset" sx={{ padding: 1, paddingTop: 3, marginBottom: 1, borderRadius: 2, borderColor: 'light' }}>
        <AuditReportTemplates required templates={ reportTemplates } onSelected={ handleTemplateSelected } />
      </Box>

      <Box component="fieldset" sx={{ padding: 1, marginBottom: 1, borderRadius: 2, borderColor: 'light' }}>
        <TextField id="name" label="Name" required variant="outlined" fullWidth margin="normal"
          value={ reportConfig.name } onChange={ e => setReportConfig({...reportConfig, name: e.target.value}) }/>

        <TextField id="description" label="Description" variant="outlined" fullWidth margin="normal" multiline rows={ 4 }
          value={ reportConfig.description } onChange={ e => setReportConfig({...reportConfig, description: e.target.value}) }/>
      </Box>

      <Box component="fieldset" sx={{ padding: 1, marginBottom: 1, borderRadius: 2, borderColor: 'light' }}>
        <legend><Typography>Transaction Source</Typography></legend>

        <FormControl fullWidth margin="normal" required>
          <InputLabel id="select-source-type">Source</InputLabel>
          <Select labelId="select-source-type" label="Source"
            value={ reportConfig.source || ''} onChange={ selectSourceType }>
            { Object.entries(sourceDescriptions).map(entry =>
              <MenuItem key={ entry[0] } value={ entry[0] }>{ entry[1] }</MenuItem>
            )}
          </Select>
        </FormControl>
  
        { reportConfig.source && reportConfig.source !== 'ALL' &&
          <FormControl fullWidth margin="normal" required={ reportSources.length > 0 } disabled={ reportSources.length === 0 }>
            <InputLabel id="select-source-id">{ sourceDescriptions[reportConfig.source] }</InputLabel>
            <Select labelId="select-source-id" label={ sourceDescriptions[reportConfig.source] }
              value={ reportConfig.sourceId || ''} onChange={ selectSourceId }>
              { reportSources }
            </Select>
          </FormControl>
        }

        { reportConfig.source && reportConfig.source === 'CATEGORY_GROUP' &&
          <FormControl fullWidth margin="normal">
            <FormControlLabel label="Include Uncategorised Transactions" control={
              <Switch id="uncategorisedIncluded" checked={ reportConfig.uncategorisedIncluded }
                onChange={ e => setReportConfig({...reportConfig, uncategorisedIncluded: e.target.checked}) }/>
            } />
          </FormControl>
        }
      </Box>

      <Box component="fieldset" sx={{ padding: 1, marginBottom: 1, borderRadius: 2, borderColor: 'light' }}>
        <legend><Typography>Parameters</Typography></legend>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Default</TableCell>
              <TableCell>Value</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            { selectedTemplate?.parameters.map(param => 
              <TableRow key={ param.name }>
                <TableCell>{ param.name }
                  <InfoPopover content={ param.description }/>
                </TableCell>
                <TableCell>{ param.defaultValue }</TableCell>
                <TableCell>
                  <TextField id={ param.name } variant="outlined" fullWidth margin="none"
                    type={ param.type === 'BOOLEAN' ? 'checkbox' : param.type === 'DOUBLE'|| param.type === 'LONG' ? 'number' : 'text' }
                    value={ reportConfig.parameters.get(param.name) || param.defaultValue || '' }
                    onChange={ e => setReportConfig({ ...reportConfig, parameters: reportConfig.parameters.set(param.name, e.target.value) }) }/>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Box>

      <Button type="submit" variant="outlined" disabled={validateForm(reportConfig).length > 0}>Save</Button>
    </form>
  )
}