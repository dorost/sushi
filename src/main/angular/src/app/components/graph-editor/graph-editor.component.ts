import {Component, ElementRef, OnInit} from '@angular/core';
import {ActionBlock, Block, StateMachine} from "../../models/StateMachine";
import {graphlib, layout} from "dagre";
import Graph = graphlib.Graph;
import * as d3 from 'd3';
import { range } from 'lodash';

type BlockSelection = d3.Selection<SVGGElement, Block, any, any>;

@Component({
  selector: 'app-graph-editor',
  templateUrl: './graph-editor.component.html',
  styleUrls: ['./graph-editor.component.scss']
})
export class GraphEditorComponent implements OnInit {
  private stateMachine: StateMachine = {
    blocks: [
      new ActionBlock('1', 'Block 1', ['2']),
      new ActionBlock('2', 'Block 2', ['3', '4']),
      new ActionBlock('3', 'Block 3', []),
      new ActionBlock('4', 'Block 4', ['5']),
      new ActionBlock('5', 'Block 5', []),
    ]
  };

  private blockRenderData: { [blockId: string]: BlockRenderData } = {};
  private svg: SVGElement;

  constructor(private elementRef: ElementRef) {}

  ngOnInit(): void {
    this.svg = (this.elementRef.nativeElement as HTMLDivElement).getElementsByTagName('svg').item(0);
    this.render(this.stateMachine);
  }

  private render(stateMachine: StateMachine) {
    this.updateBlockRenderData(stateMachine, {pointsPerSide: 3, padding: 20, radius: 4});

    const blockRenderData = this.blockRenderData;

    const graph: Graph = new graphlib.Graph();
    graph.setGraph({});
    graph.setDefaultEdgeLabel(function() { return {}; });
    stateMachine.blocks.forEach(block => {
      graph.setNode(block.id, blockRenderData[block.id]);
    });
    stateMachine.blocks.forEach(block => {
      (block as ActionBlock).nextBlocks.forEach(nextBlock => {
        graph.setEdge(block.id, nextBlock);
      });
    });

    layout(graph);

    const frame = d3.select(this.svg)
      .append('g');

    const blocks = frame.selectAll('g.block')
      .data(stateMachine.blocks);

    const enteringBlocks = blocks.enter()
      .append('g')
      .attr('class', 'block')
      .attr('transform', (b: Block) =>
        `translate(${blockRenderData[b.id].x},${blockRenderData[b.id].y})`);

    enteringBlocks
      .append('rect')
      .attr('class', 'block')
      .attr('width', (b: Block) => blockRenderData[b.id].width)
      .attr('height', (b: Block) => blockRenderData[b.id].height);


    enteringBlocks
      .each(function(b: Block) {
        d3.select(this)
          .selectAll('circle.connection-point')
          .data(blockRenderData[b.id].connectionPoints)
          .enter()
          .append('circle')
          .attr('class', 'connection-point')
          .attr('cx', (p: ConnectionPoint) => p.x)
          .attr('cy', (p: ConnectionPoint) => p.y)
          .attr('r', (p: ConnectionPoint) => p.r)
      });

    enteringBlocks
      .append('text')
      .attr('text-anchor', 'middle')
      .attr('alignment-baseline', 'middle')
      .attr('dx', (b: Block) => this.blockRenderData[b.id].width / 2)
      .attr('dy', (b: Block) => this.blockRenderData[b.id].height / 2)
      .text((b: Block) => b.name);

    console.log(this.blockRenderData);
  }

  private updateBlockRenderData(stateMachine: StateMachine, connectionPointConfig: ConnectionPointConfig) {
    stateMachine.blocks.forEach(block => {
      if (!(block.id in this.blockRenderData)) {
        const width = 100;
        const height = 100;
        const connectionPoints: ConnectionPoint[] = [];
        range(connectionPointConfig.pointsPerSide).forEach(i => {
          connectionPoints.push({
            x: this.pointPosition(connectionPointConfig, height, i),
            y: 0,
            r: connectionPointConfig.radius
          });
          connectionPoints.push({
            x: this.pointPosition(connectionPointConfig, height, i),
            y: width,
            r: connectionPointConfig.radius
          });
          connectionPoints.push({
            x: 0,
            y: this.pointPosition(connectionPointConfig, width, i),
            r: connectionPointConfig.radius
          });
          connectionPoints.push({
            x: height,
            y: this.pointPosition(connectionPointConfig, width, i),
            r: connectionPointConfig.radius
          });
        });
        this.blockRenderData[block.id] = { width, height, x: 0, y: 0, connectionPoints };
      }
    });
  }

  private pointPosition(config: ConnectionPointConfig, totalSize: number, pointNr: number) {
    if (config.pointsPerSide === 1) {
      return totalSize / 2;
    }
    return config.padding + pointNr * (totalSize - 2 * config.padding) / (config.pointsPerSide - 1);
  }
}

interface ConnectionPointConfig {
  pointsPerSide: number;
  padding: number;
  radius: number;
}

interface BlockRenderData {
  width: number;
  height: number;
  x: number;
  y: number;
  connectionPoints: ConnectionPoint[];
}

interface ConnectionPoint {
  x: number;
  y: number;
  r: number;
}
