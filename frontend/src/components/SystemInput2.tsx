import React, { useContext } from 'react';
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Box,
  FormControl,
  IconButton,
  InputAdornment,
  InputLabel,
  List,
  ListItem,
  OutlinedInput,
} from "@material-ui/core";
import { makeStyles } from "@material-ui/core/styles";
import CloseIcon from '@material-ui/icons/Close';
import { GlobalDataContext } from "../GlobalDataContext";
import axios from 'axios';
import { ResponseSystems } from "../response";

const useStyles = makeStyles((theme) => ({
  wrap: {
    position: "relative",
    maxWidth: "300px",
  },
  form: {
  },
  box: {
    position: "absolute",
    zIndex: 100,
    width: "100%",
    marginTop: "3px",
    backgroundColor: theme.palette.background.paper,
    borderRadius: "4px",
    maxHeight: "250px", // TODO dyn. height + open above/below
    overflowY: "auto",
  },
  list: {
  }
}));

type Props = {
  fieldId: string,
  fieldName: string,
  onChange: Function,
}

export default function Search(props: Props) {
  const { t } = useTranslation();
  const globalData = useContext(GlobalDataContext);
  const classes = useStyles();
  const [searchTerm, setSearchTerm] = useState('');
  const [systems, setSystems] = useState<string[]>([]);
  const [open, setOpen] = useState(false);
  const [inputValue, setInputValue] = useState('');

  useEffect(() => {
    const delayDebounceFn = setTimeout(() => {
      if (searchTerm === '') {
        setSystems([]);
        setOpen(false);
        return;
      }

      (async () => {
        const response = await axios.get<ResponseSystems>(`${globalData.domain}/api/systems/find/${searchTerm}`);
        setSystems(response.data.systems);
        setOpen(true);
      })();

    }, 300);

    return () => clearTimeout(delayDebounceFn)
  }, [globalData.domain, searchTerm]);

  const onChange = (value: string) => {
    setInputValue(value);
    setSearchTerm(value);
    props.onChange(value);
  };

  const selectSystem = (e: React.MouseEvent) => {
    const system = e.currentTarget.textContent + '';
    setOpen(false);
    setInputValue(system);
    props.onChange(system);
  };

  return (
    <div className={classes.wrap}>
      <FormControl className={classes.form} variant="outlined">
        <InputLabel htmlFor={props.fieldId}>{props.fieldName}</InputLabel>
        <OutlinedInput
          id={props.fieldId}
          type="search"
          autoComplete="off"
          value={inputValue}
          onChange={(e) => onChange(e.target.value)}
          endAdornment={ // TODO smaller, grey, hover = white
            <InputAdornment position="end">
              <IconButton title={t('systemInput.clear')} onClick={() => onChange("")} edge="end">
                {inputValue !== '' ? <CloseIcon /> : ''}
              </IconButton>
            </InputAdornment>
          }
          labelWidth={70}
        />
      </FormControl>
      {open &&
        <Box boxShadow={2} className={classes.box}>
          <List className={classes.list}>
            {systems.map((value, index) => {
              if (index > 100) { // TODO better way to prevent lag?
                return '';
              }
              return (
                <ListItem button key={index} onClick={selectSystem}>{value}</ListItem>
              )
            })}
          </List>
        </Box>
      }
    </div>
  )
}
