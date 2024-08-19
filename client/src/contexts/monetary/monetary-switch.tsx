import Switch from '@mui/material/Switch';
import Avatar from '@mui/material/Avatar';
import Tooltip from '@mui/material/Tooltip';

import MoneyOnIcon from '@mui/icons-material/AttachMoney';
import MoneyOffIcon from '@mui/icons-material/MoneyOff';

import useMonetaryContext from './monetary-context'

function CheckedIcon() {
  return (
    <Avatar sx={{ width: 22, height: 22, bgcolor: '#e3f2fd', color: 'black' }}><MoneyOffIcon /></Avatar>
  )
}

function UncheckedIcon() {
  return (
    <Avatar sx={{ width: 22, height: 22, bgcolor: '#e3f2fd', color: 'black' }}><MoneyOnIcon /></Avatar>
  )
}

function getTooltip(hidden: boolean) {
  return (hidden ? 'Show' : 'Hide') + ' monetary figures';
}

export default function MonetarySwitch() {
  // eslint-disable-next-line
  const [ _, hidden, setHidden ] = useMonetaryContext();
  
  return (
    <Tooltip title={ getTooltip(hidden) } placement='bottom'>
      <Switch
        color='default'
        icon={ <UncheckedIcon /> }
        checkedIcon={ <CheckedIcon/> }
        value={ hidden }
        onChange={ e => setHidden(e.target.checked) }
      ></Switch>
    </Tooltip>
  )
}