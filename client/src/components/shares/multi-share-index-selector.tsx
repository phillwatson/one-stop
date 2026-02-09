import { useEffect, useMemo, useState } from "react";
import { Checkbox, FormControl, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Typography } from "@mui/material";

import { ShareIndex } from "../../model/share-indices.model";

interface Props {
  shareIndices: ShareIndex[],
  excludedIndices?: ShareIndex[],
  label?: string,
  onSelectIndices?: (value: ShareIndex[]) => void;
}

export default function MultiShareIndexSelector(props: Props) {
  // a list of items for selection - without the exclusions
  const listedItems = useMemo(() => {
    if (props.excludedIndices === undefined || props.excludedIndices.length === 0) {
      return props.shareIndices;
    }
    return props.shareIndices.filter(item => props.excludedIndices!.findIndex(excl => excl.id === item.id) === -1)
  }, [ props.shareIndices, props.excludedIndices]);

  const [ selectedIndices, setSelectedIndices ] = useState<ShareIndex[]>([]);

  function selectIndex(shareIndex: ShareIndex) {
    if (selectedIndices.findIndex(s => s.id === shareIndex.id) >= 0) {
      setSelectedIndices(prev => prev.filter(item => item.id !== shareIndex.id));
    } else {
      setSelectedIndices(prev => [...prev, shareIndex]);
    }
  }

  // call the callback on selection update
  const callback = props.onSelectIndices;
  useEffect(() => {
    if (callback) {
      callback(selectedIndices);
    }
  }, [ callback, selectedIndices ]);

  return (
      <FormControl fullWidth>
        <Typography gutterBottom noWrap={ true }> { props.label || 'Select Indices' } </Typography>

        <List sx={{ height: '250px', overflow: 'auto', border: '1px solid lightgrey', borderRadius: '1%' }}>
          { listedItems.map(item =>
            <ListItem key={ item.id } dense disableGutters disablePadding>
              <ListItemButton role={ undefined } onClick={() => selectIndex(item) } dense sx={{ height: "24pt" }}>
                <ListItemIcon>
                  <Checkbox
                    edge="start" tabIndex={-1} 
                    checked={ selectedIndices.find(i => i.id === item.id) !== undefined }
                  />
                </ListItemIcon>

                <ListItemText id={ item.id } key={ item.id } primary={ item.name } />
              </ListItemButton>
            </ListItem>
          )}
        </List>
      </FormControl>
    
  );
}